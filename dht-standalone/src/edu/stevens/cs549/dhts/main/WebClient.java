package edu.stevens.cs549.dhts.main;

import java.net.URI;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBElement;

import edu.stevens.cs549.dhts.activity.DHTBase;
import edu.stevens.cs549.dhts.activity.DHTBase.Failed;
import edu.stevens.cs549.dhts.activity.NodeInfo;
import edu.stevens.cs549.dhts.resource.TableRep;

public class WebClient {

	private Logger log = Logger.getLogger(WebClient.class.getCanonicalName());

	private void error(String msg) {
		log.severe(msg);
	}

	/*
	 * Encapsulate Web client operations here.
	 * 
	 * TODO: Fill in missing operations.
	 */

	/*
	 * Creation of client instances is expensive, so just create one.
	 */
	protected Client client;
	
	public WebClient() {
		client = ClientBuilder.newClient();
	}

	private void info(String mesg) {
		Log.info(mesg);
	}

	private Response getRequest(URI uri) {
		try {
			Response cr = client.target(uri)
					.request(MediaType.APPLICATION_XML_TYPE)
					.header(Time.TIME_STAMP, Time.advanceTime())
					.get();
			processResponseTimestamp(cr);
			return cr;
		} catch (Exception e) {
			error("Exception during GET request: " + e);
			return null;
		}
	}

	private Response putRequest(URI uri, Entity<?> entity) {
		try {
			Response cr = client.target(uri)
					.request()
					.header(Time.TIME_STAMP, Time.advanceTime())
					.put(entity);
			processResponseTimestamp(cr);
			return cr;
		} catch (Exception e) {
			error("Exception during POST request: " + e);
			return null;
		}
		
	}
	
	private Response deleteRequest(URI uri) {
		try {
			Response cr = client.target(uri)
					.request()
					.header(Time.TIME_STAMP, Time.advanceTime())
					.delete();
			processResponseTimestamp(cr);
			return cr;
		} catch (Exception e) {
			error("Exception during POST request: " + e);
			return null;
		}
		
	}
	
	private Response putRequest(URI uri) {
		return putRequest(uri, Entity.text(""));
	}

	private void processResponseTimestamp(Response cr) {
		Time.advanceTime(Long.parseLong(cr.getHeaders().getFirst(Time.TIME_STAMP).toString()));
	}

	/*
	 * Jersey way of dealing with JAXB client-side: wrap with run-time type
	 * information.
	 */
	private GenericType<JAXBElement<NodeInfo>> nodeInfoType = new GenericType<JAXBElement<NodeInfo>>() {
	};
	
	private GenericType<JAXBElement<String[]>> stringArray = new GenericType<JAXBElement<String[]>>() {
	};
	

	/*
	 * Ping a remote site to see if it is still available.
	 */
	public boolean isFailed(URI base) {
		URI uri = UriBuilder.fromUri(base).path("info").build();
		Response c = getRequest(uri);
		return c == null || c.getStatus() >= 300;
	}

	/*
	 * Get the predecessor pointer at a node.
	 */
	public NodeInfo getPred(NodeInfo node) throws DHTBase.Failed {
		URI predPath = UriBuilder.fromUri(node.addr).path("pred").build();
		info("client getPred(" + predPath + ")");
		Response response = getRequest(predPath);
		if (response == null || response.getStatus() >= 300) {
			throw new DHTBase.Failed("GET /pred");
		} else {
			NodeInfo pred = response.readEntity(nodeInfoType).getValue();
			return pred;
		}
	}

	/*
	 * @author Wen Zhang
	 */
	public NodeInfo getSucc(NodeInfo node) throws DHTBase.Failed {
		URI succPath = UriBuilder.fromUri(node.addr).path("succ").build();
		info("client getSucc(" + succPath + ")");
		Response response = getRequest(succPath);
		if (response == null || response.getStatus() >= 300) {
			throw new DHTBase.Failed("GET /succ");
		} else {
			NodeInfo succ = response.readEntity(nodeInfoType).getValue();
			return succ;
		}
	}
	
	/*
	 * @author Wen Zhang
	 */
	public NodeInfo getFinger(URI uri, int index) throws DHTBase.Failed {
		URI fingerPath = UriBuilder.fromUri(uri).path("finger").queryParam("index", index).build();
		info("client getFinger(" + fingerPath + ")");
		Response response = getRequest(fingerPath);
		if (response == null || response.getStatus() >= 300) {
			throw new DHTBase.Failed("GET /finger");
		} else {
			NodeInfo succ = response.readEntity(nodeInfoType).getValue();
			return succ;
		}
	}
	
	/*
	 * Notify node that we (think we) are its predecessor.
	 */
	public TableRep notify(NodeInfo node, TableRep predDb) throws DHTBase.Failed {
		/*
		 * The protocol here is more complex than for other operations. We
		 * notify a new successor that we are its predecessor, and expect its
		 * bindings as a result. But if it fails to accept us as its predecessor
		 * (someone else has become intermediate predecessor since we found out
		 * this node is our successor i.e. race condition that we don't try to
		 * avoid because to do so is infeasible), it notifies us by returning
		 * null. This is represented in HTTP by RC=304 (Not Modified).
		 */
		NodeInfo thisNode = predDb.getInfo();
		UriBuilder ub = UriBuilder.fromUri(node.addr).path("notify");
		URI notifyPath = ub.queryParam("id", thisNode.id).build();
		info("client notify(" + notifyPath + ")");
		Response response = putRequest(notifyPath, Entity.xml(predDb));
		if (response != null && response.getStatusInfo() == Response.Status.NOT_MODIFIED) {
			/*
			 * Do nothing, the successor did not accept us as its predecessor.
			 */
			return null;
		} else if (response == null || response.getStatus() >= 300) {
			throw new DHTBase.Failed("PUT /notify?id=ID");
		} else {
			TableRep bindings = response.readEntity(TableRep.class);
			return bindings;
		}
	}
	
	public NodeInfo findClosestPrecedeFinger(URI addr, int id) throws Failed {
		URI succOfIdURI = UriBuilder.fromUri(addr).path("closestfinger").queryParam("id", id).build();
		info("client findClosestPrecedeFinger(" + succOfIdURI + ")");
		Response response = getRequest(succOfIdURI);
		if (response == null || response.getStatus() >= 300) {
			throw new DHTBase.Failed("GET /pred");
		} else {
			NodeInfo precedeFinger = response.readEntity(nodeInfoType).getValue();
			return precedeFinger;
		}
	}
	
	public NodeInfo findSuccessor(URI addr, int id) throws Failed {
		URI succOfIdURI = UriBuilder.fromUri(addr).path("find").queryParam("id", id).build();
		info("client getPred(" + succOfIdURI + ")");
		Response response = getRequest(succOfIdURI);
		if (response == null || response.getStatus() >= 300) {
			throw new DHTBase.Failed("GET /pred");
		} else {
			NodeInfo successor = response.readEntity(nodeInfoType).getValue();
			return successor;
		}
	}
	
	/*
	 * Get Bindings in Network; Update Bindings in Network; Delete Bindings in Network;
	 */
	
	public String[] getKey(URI addr, String key) throws Failed {
		URI succOfIdURI = UriBuilder.fromUri(addr).queryParam("key", key).build();
		info("client getKey(" + succOfIdURI + ")");
		Response response = getRequest(succOfIdURI);
		if (response == null || response.getStatus() >= 300) {
			throw new DHTBase.Failed("GET /getKey");
		} else {
			String[] values = response.readEntity(stringArray).getValue();
			return values;
		}
	}
	
	public void putKey(URI addr, String key, String value) throws Failed {
		URI succOfIdURI = UriBuilder.fromUri(addr).queryParam("key", key).queryParam("value", value).build();
		info("client putKey(" + succOfIdURI + ")");
		Response response = putRequest(succOfIdURI);
		if (response == null || response.getStatus() >= 300) {
			throw new DHTBase.Failed("GET /putKey");
		} 
	}

	public void deleteKey(URI addr, String key, String value) throws Failed {
		URI succOfIdURI = UriBuilder.fromUri(addr).queryParam("key", key).queryParam("value", value).build();
		info("client deleteKey(" + succOfIdURI + ")");
		Response response = deleteRequest(succOfIdURI);
		if (response == null || response.getStatus() >= 300) {
			throw new DHTBase.Failed("GET /putKey");
		} 
	}
	
}
