/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stevens.cs549.ftpclient;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.log4j.PropertyConfigurator;

import edu.stevens.cs549.ftpinterface.IServer;
import edu.stevens.cs549.ftpinterface.IServerFactory;

/**
 * 
 * @author dduggan
 */
public class Client {

	private static String clientPropsFile = "/client.properties";
	private static String loggerPropsFile = "/log4j.properties";
	
	private static String BASE_DIR;
	
	public static Logger log = Logger.getLogger("edu.stevens.cs.cs549.ftpclient");
	
    static final int MAX_PATH_LEN = 1024;
    
	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		new Client();
	}
	
	InetAddress serverAddress;
	
	static InetAddress localAddress;

	public Client() {
		try {
			PropertyConfigurator.configure(getClass().getResource(loggerPropsFile));
			/*
			 * Load server properties.
			 */
			Properties props = new Properties();
			InputStream in = getClass().getResourceAsStream(clientPropsFile);
			props.load(in);
			in.close();
			
			String serverMachine = (String) props.get("server.machine");
			serverMachine = "ec2-52-1-197-146.compute-1.amazonaws.com";
			log.info(serverMachine);
			
			String serverName = (String) props.get("server.name");
			log.info(serverName);
			int serverPort = Integer.parseInt((String) props.get("server.port"));
			/*
			 * set up inner address for active mode
			 */
			localAddress = InetAddress.getByName((String) props.get("client.ip"));
			
			/*
			 * TODO: Get a server proxy.
			 */
			BASE_DIR = (String)props.get("client.workhome") + "/";
        	String serverIp = (String)props.get("server.ip");
        	/*
        	 * Register factory object in registry.
        	 */
        	
        	log.info("Looking for registry in " + serverMachine + ":" + serverPort);
            Registry registry = LocateRegistry.getRegistry(serverMachine, serverPort);
            log.info("find the rmi registry ....");
            IServerFactory serverFactory = (IServerFactory) registry.lookup(serverName);
            log.info("looked up the Object for [" + serverName + "]");
            IServer server = serverFactory.createServer();
            log.info("Force Server to create a thread for client!");
			/*
			 * Start CLI.  Second argument should be server proxy.
			 */
			cli(serverMachine, server);

		} catch (java.io.FileNotFoundException e) {
			log.severe("Client error: " + clientPropsFile + " file not found.");
		} catch (java.io.IOException e) {
			log.severe("Client error: IO exception.");
			e.printStackTrace();
		} catch (Exception e) {
			log.severe("Client exception:");
			e.printStackTrace();
		}

	}

	static void msg(String m) {
		System.out.print(m);
	}

	static void msgln(String m) {
		System.out.println(m);
	}

	static void err(Exception e) {
		System.err.println("Error : "+e);
		e.printStackTrace();
	}

	public static void cli(String svrHost, IServer svr) {

		// Main command-line interface loop

		try {
			InetAddress serverAddress = InetAddress.getByName(svrHost);
			Dispatch d = new Dispatch(svr, serverAddress);
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

			while (true) {
				msg("ftp> ");
				String line = in.readLine();
				String[] inputs = line.split("\\s+");
				if (inputs.length > 0) {
					String cmd = inputs[0];
					if (cmd.length()==0)
						;
					else if ("get".equals(cmd))
						d.get(inputs);
					else if ("put".equals(cmd))
						d.put(inputs);
					else if ("cd".equals(cmd))
						d.cd(inputs);
					else if ("pwd".equals(cmd))
						d.pwd(inputs);
					else if ("dir".equals(cmd))
						d.dir(inputs);
					else if ("ldir".equals(cmd))
						d.ldir(inputs);
					else if ("port".equals(cmd))
						d.port(inputs);
					else if ("pasv".equals(cmd))
						d.pasv(inputs);
					else if ("help".equals(cmd))
						d.help(inputs);
					else if ("quit".equals(cmd))
						return;
					else
						msgln("Bad input.  Type \"help\" for more information.");
				}
			}
		} catch (EOFException e) {
		} catch (UnknownHostException e) {
			err(e);
			System.exit(-1);
		} catch (IOException e) {
			err(e);
			System.exit(-1);
		}
		

	}

	public static class Dispatch {

		private IServer svr;
		
		private InetAddress serverAddress;

		Dispatch(IServer s, InetAddress sa) {
			svr = s;
			serverAddress = sa;
		}

		public void help(String[] inputs) {
			if (inputs.length == 1) {
				msgln("Commands are:");
				msgln("  get filename: download file from server");
				msgln("  put filename: upload file to server");
				msgln("  pwd: current working directory on server");
				msgln("  cd filename: change working directory on server");
				msgln("  dir: list contents of working directory on server");
				msgln("  ldir: list contents of current directory on client");
				msgln("  port: server should transfer files in active mode");
				msgln("  pasv: server should transfer files in passive mode");
				msgln("  quit: exit the client");
			}
		}

		/*
		 * ********************************************************************************************
		 * Data connection.
		 */

		enum Mode {
			NONE, PASSIVE, ACTIVE
		};

		/*
		 * Note: This refers to the mode of the SERVER.
		 */
		private Mode mode = Mode.NONE;

		/*
		 * If active mode, remember the client socket.
		 */

		private ServerSocket dataChan = null;

		private InetSocketAddress makeActive() throws IOException {
//			dataChan = new ServerSocket(0);
			dataChan = new ServerSocket(0,5, localAddress);
			mode = Mode.ACTIVE;
			/* 
			 * Note: this only works (for the server) if the client is not behind a NAT.
			 */
			log.info("Client is building a socket " + dataChan.getLocalSocketAddress());
			return (InetSocketAddress) (dataChan.getLocalSocketAddress());
		}

		/*
		 * If passive mode, remember the server socket address.
		 */
		private InetSocketAddress serverSocket = null;

		private void makePassive(InetSocketAddress s) {
			log.info(s.toString());
			serverSocket = s;
			mode = Mode.PASSIVE;
		}

		/*
		 * *********************************************************************************************
		 */

		private class ActiveDownloadThread implements Runnable {
			/*
			 * This client-side thread runs when the server is active mode and a
			 * file download is initiated. This thread listens for a connection
			 * request from the server. The client-side server socket (...)
			 * should have been created when the port command put the server in
			 * active mode.
			 */
			
			private String file_name;
			
			private String current_path;
			

			public ActiveDownloadThread(String current_path, String file_name) throws IOException {
				this.file_name = file_name;
				this.current_path = current_path;
			}

			public void run() {
				try {
					/*
					 * TODO: Complete this thread.
					 */
					
					Socket xfer;
					synchronized (Client.class) {
						new Thread(new Runnable() {
							
							public void run() {
								try {
									svr.get(current_path + file_name);
								} catch (FileNotFoundException e) {
									e.printStackTrace();
								} catch (RemoteException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}).start();
						
						xfer = dataChan.accept();
					}
					
					long total_size = svr.length(current_path + file_name);
					long current_read = 0;
					
					File dir = new File(BASE_DIR + current_path); // local dir
					if(!dir.exists()) {
						dir.mkdirs();
					}
					
					OutputStream file = new FileOutputStream(BASE_DIR + current_path + file_name);
					
					InputStream socketInputStream = xfer.getInputStream();
					
					byte[] buf = new byte[MAX_PATH_LEN];
					int readBytes = socketInputStream.read(buf, 0, MAX_PATH_LEN);
					
					int count = 0;
					while(readBytes > 0) {
						file.write(buf, 0, readBytes < MAX_PATH_LEN ? readBytes : MAX_PATH_LEN);
						
						current_read = current_read + readBytes;
						if(count % 1000 == 0) {
							msg( "\n [Download] active " + BASE_DIR + current_path + file_name + " from " + current_path + file_name  + "  downloading " + caculte(current_read, total_size) + "\n" );
						}
						
						readBytes = socketInputStream.read(buf, 0, MAX_PATH_LEN);
						
						count++;
					}
					
					msg( "\n[Download] active " + BASE_DIR + current_path + file_name + " from " + current_path + file_name  + "  downloading %" + caculte(current_read, total_size) + "\n" );
					msg("ftp>" );
					
					file.close();
					socketInputStream.close();
					xfer.close();
					/*
					 * End TODO
					 */
				} catch (IOException e) {
					msg("Exception: " + e);
					e.printStackTrace();
				}
			}
		}
		
		/**
		 * Pasv Download Mode
		 */
		private class PasvDownloadThread implements Runnable {
			
			private String file;
			
			private String path;
			
			public PasvDownloadThread(String path, String file) {
				this.file = file;
				this.path = path;
			}

			public void run() {
				try {
					Socket xfer;
					synchronized (Client.class) {
						// force server in passive mode and create a port to listen
						svr.get(path + file);
						
						// force server to listening on that port 
						xfer = new Socket(serverAddress, serverSocket.getPort());
						
						log.info("CURRENT LOCAL ADDRESS IS " + xfer.getLocalSocketAddress() + " REMOTE PORT IS " + xfer.getRemoteSocketAddress());
					}
					
					long total_size = svr.length(path + file);
					long current_read = 0;
					
					//mkdirs, if dirs not exist
					File dir = new File(BASE_DIR + path);
					if(!dir.exists()) {
						dir.mkdirs();
					}
					
					// build local Fileoutput Stream and receive the Socket Input!
					FileOutputStream fileOutputOS = new FileOutputStream(BASE_DIR + path + file);
					
					InputStream socktDataIS = xfer.getInputStream();
					byte[] buf = new byte[MAX_PATH_LEN];
					int readbytes = socktDataIS.read(buf, 0, MAX_PATH_LEN);
					
					int count = 1;
					while(readbytes > 0) {
						fileOutputOS.write(buf, 0, readbytes < MAX_PATH_LEN ? readbytes : MAX_PATH_LEN);
						current_read = readbytes + current_read;
						
						if(count % 1000 == 0) {
							msg( "\n [Download] pasv " + BASE_DIR + path + file + " from " + path + file  + "  downloading " + caculte(current_read, total_size) );
						}
						readbytes = socktDataIS.read(buf, 0, MAX_PATH_LEN);
						count++;
					}
					
					msg( "\n[Download] pasv " + path + file + " to " + BASE_DIR + path + file  + "  downloading %" + caculte(current_read, total_size)+ "\n" );
					msg("ftp>" );
					
					xfer.close();
					socktDataIS.close();
					fileOutputOS.close();
					
				} catch (IOException e) {
					msg("Exception: " + e);
					e.printStackTrace();
				}
			}
			
		}
		
		
		/**
		 * Recursive Copy file into Local machine
		 */
		private void recursiveDownload(String path) throws IOException, RemoteException {
			if(svr.isDirectory(path)) {
				//client build a folder in basedir
				File dir = new File(BASE_DIR + svr.pwd() + path);
				if(!dir.exists()) {
					dir.mkdirs();
				}
				
				//Connect server to retrieve back the file
				svr.cd(path);
				String[] currentItems = svr.dir();
				String current_path = svr.pwd();
				for(String item : currentItems) {
					if(svr.isDirectory(item)) {
						recursiveDownload(item);
					} else {
						//If the item is a file, then build connection and receive stream
						/*
						 * PASSIVE MODE : WITH pasv download Thread
						 */
						if(mode == Mode.PASSIVE) {
							new Thread(new PasvDownloadThread(current_path, item)).start();
						} else if (mode == Mode.ACTIVE) {
							
							/*
							 * Active MODE : WITH active download Thread
							 */	
							new Thread(new ActiveDownloadThread(current_path, item)).start();
						} 
					}
				}
				
				//after retrieve all the subfolder, back to the parent folder
				svr.cd("..");
			} else {
				if(mode == Mode.PASSIVE) {
					new Thread(new PasvDownloadThread(svr.pwd(), path)).start();;
				} else if (mode == Mode.ACTIVE) {
					new Thread(new ActiveDownloadThread(svr.pwd(), path)).start();
				} 
				
			}
		}

		public void get(String[] inputs) {
			if (inputs.length == 2) {
				try {
					if(mode == Mode.PASSIVE || mode == Mode.ACTIVE) {
						recursiveDownload(inputs[1]);
					} else {
						msgln("GET: No mode set--use port or pasv command.");
					}
						
				} catch (Exception e) {
					err(e);
				}
			}
		}

		

		/**
		 * Pasv Download Mode
		 */
		private class PasvUploadThread implements Runnable {
			
			private String file;
			
			private String path;
			
			private String serverPath;
			
			
			public PasvUploadThread(String path, String file, String serverPath) {
				this.file = file;
				this.path = path;
				this.serverPath = serverPath;
			}

			public void run() {
//				log.info("BEGIN TO UPLOADT " + serverPath);
				try {
					Socket xfer;
//					synchronized (Client.class) {
						
						//force server to building a server socket
						svr.put(serverPath);
						
						// force server to listening on that port
						xfer = new Socket(serverAddress, serverSocket.getPort());
//					}
					
					// build local Fileoutput Stream and receive the Socket Input!
					FileInputStream fileOutputIS = new FileInputStream(BASE_DIR + path + file);
					long total_size = new File(BASE_DIR + path + file).length();
					long current_write = 0;
					
					OutputStream socktDataOS = xfer.getOutputStream();
					byte[] buf = new byte[MAX_PATH_LEN];
					int readbytes = fileOutputIS.read(buf, 0, MAX_PATH_LEN);
					log.info("client begin to read!!");
					int count = 1;
					while(readbytes > 0) {
						socktDataOS.write(buf, 0, readbytes < MAX_PATH_LEN ? readbytes : MAX_PATH_LEN);
						current_write = readbytes + current_write;
						
						if(count % 1000 == 0) {
							msg( "\n[Upload]" + BASE_DIR + path + file  + " to " + serverPath + "  uploading " + caculte(current_write, total_size));
						}
						readbytes = fileOutputIS.read(buf, 0, MAX_PATH_LEN);
						count++;
					}
					
					msg( "\n[Upload]" + BASE_DIR + path + file + " to " + serverPath  + "  uploading " + caculte(current_write, total_size) + "\n" );
					msg("ftp>" );
					
					xfer.close();
					socktDataOS.close();
					fileOutputIS.close();
					
				} catch (IOException e) {
					msg("Exception: " + e);
					e.printStackTrace();
				}
			}
			
		}
		
		/**
		 * Pasv Download Mode
		 */
		private class ActiveUploadThread implements Runnable {
			
			private String file;
			
			private String path;
			
			private String serverPath;
			
			
			public ActiveUploadThread(String path, String file, String serverPath) {
				this.file = file;
				this.path = path;
				this.serverPath = serverPath;
			}

			public void run() {
//				log.info("BEGIN TO UPLOADT " + serverPath);
				try {
					Socket xfer;
					synchronized (Client.class) {
						
						new Thread(new Runnable() {
							public void run() {
								try {
									// force server to listening on the socket on that port
									svr.put(serverPath);
								} catch (FileNotFoundException e) {
									e.printStackTrace();
								} catch (RemoteException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}).start();
						
						// client build a monitor on listening  on the port
						xfer = dataChan.accept();
					}
					
					// build local Fileoutput Stream and receive the Socket Input!
					FileInputStream fileOutputIS = new FileInputStream(BASE_DIR + path + file);
					long total_size = new File(BASE_DIR + path + file).length();
					long current_write = 0;
					
					OutputStream socktDataOS = xfer.getOutputStream();
					byte[] buf = new byte[MAX_PATH_LEN];
					int readbytes = fileOutputIS.read(buf, 0, MAX_PATH_LEN);
					
					int count = 1;
					while(readbytes > 0) {
						socktDataOS.write(buf, 0, readbytes < MAX_PATH_LEN ? readbytes : MAX_PATH_LEN);
						current_write = readbytes + current_write;
						
						if(count % 1000 == 0) {
							msg( "\n[Upload] active " + BASE_DIR + path + file  + " to " + serverPath + "  uploading %" + caculte(current_write, total_size) );
						}
						readbytes = fileOutputIS.read(buf, 0, MAX_PATH_LEN);
						count++;
					}
					
					msg( "\n[Upload] active " + BASE_DIR + path + file + " to " + serverPath  + "  uploading " + caculte(current_write, total_size) + "\n" );
					msg("ftp>" );
					
					xfer.close();
					socktDataOS.close();
					fileOutputIS.close();
					
				} catch (IOException e) {
					msg("Exception: " + e);
					e.printStackTrace();
				}
			}
			
		}
		
		private String caculte(long current, long total) {
			if(total == 0) {
				return "100%";
			} else {
				return (float)current/total * 100 + "%";
			}
		}

		/**
		 * Recursive Copy file into Local machine
		 */
		private void recursiveUpload(String file_name) throws IOException, RemoteException {
			if(isDirectory(file_name)) {
				//client build a folder in basedir
				File dir = new File(BASE_DIR + file_name);
				
				//Connect server to retrieve back the file
				//svr.cd(path);
				String current_dir = file_name;
//				String current_dir_name = file_name.substring(file_name.lastIndexOf("/") < 0 ? 0 : file_name.lastIndexOf("/") + 1, file_name.length());
				String[] currentItems = dir.list();
				String currentpwd = svr.pwd();
				svr.mkdirs(current_dir);
				for(String item : currentItems) {
					if(isDirectory(current_dir + "/" + item)) {
						recursiveUpload(current_dir + "/" + item);
					} else {
						//If the item is a file, then build connection and receive stream
						if( mode == Mode.PASSIVE) {
							new Thread(new PasvUploadThread(current_dir + "/", item, currentpwd + current_dir + "/" + item)).start();
						} else if( mode == Mode.ACTIVE) {
							new Thread(new ActiveUploadThread(current_dir + "/", item, currentpwd + current_dir + "/" + item)).start();
						}
						
					}
				}
				
				//after retrieve all the subfolder, back to the parent folder
			} else {
				String file = file_name.substring(file_name.lastIndexOf("/") < 0 ? 0 : file_name.lastIndexOf("/") + 1, file_name.length());
				
				if( mode == Mode.PASSIVE) {
					new Thread(new PasvUploadThread("/", file_name, svr.pwd() + file)).start();
				} else if( mode == Mode.ACTIVE) {
					new Thread(new ActiveUploadThread("/", file_name, svr.pwd() + file)).start();
				}
			} 
		}
		
		private Boolean isDirectory(String file) throws IOException {
			File f = new File(BASE_DIR + file);
			if(!f.exists()) {
				throw new IOException("NOT FOUND " + BASE_DIR + " " + file);
			}
			return f.isDirectory();
		}
		

		public void put(String[] inputs) {
			if (inputs.length == 2) {
				try {
					if (mode == Mode.PASSIVE || mode == Mode.ACTIVE) {

						recursiveUpload(inputs[1]);
						
					} else {
						msgln("GET: No mode set--use port or pasv command.");
					}
				} catch (Exception e) {
					err(e);
				}
			}
		}

		public void cd(String[] inputs) {
			if (inputs.length == 2)
				try {
					svr.cd(inputs[1]);
					msgln("CWD: "+svr.pwd());
				} catch (Exception e) {
					err(e);
				}
		}

		public void pwd(String[] inputs) {
			if (inputs.length == 1)
				try {
					msgln("CWD: "+svr.pwd());
				} catch (Exception e) {
					err(e);
				}
		}

		public void dir(String[] inputs) {
			if (inputs.length == 1) {
				try {
					String[] fs = svr.dir();
					for (int i = 0; i < fs.length; i++) {
						msgln(fs[i]);
					}
				} catch (Exception e) {
					err(e);
				}
			}
		}

		public void pasv(String[] inputs) {
			if (inputs.length == 1) {
				try {
					makePassive(svr.pasv());
					msgln("PASV: Server in passive mode.");
				} catch (Exception e) {
					err(e);
				}
			}
		}

		public void port(String[] inputs) {
			if (inputs.length == 1) {
				try {
					InetSocketAddress s = makeActive();
					svr.port(s);
					msgln("PORT: Server in active mode.");
				} catch (Exception e) {
					err(e);
				}
			}
		}

		public void ldir(String[] inputs) {
			if (inputs.length == 1) {
				String[] fs = new File(".").list();
				for (int i = 0; i < fs.length; i++) {
					msgln(fs[i]);
				}
			}
		}

	}

}
