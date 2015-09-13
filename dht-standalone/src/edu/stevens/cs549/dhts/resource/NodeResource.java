package edu.stevens.cs549.dhts.resource;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import edu.stevens.cs549.dhts.activity.DHTBase.Invalid;

@Path("/dht")
public class NodeResource {

	/*
	 * Web service API.
	 * 
	 * TODO: Fill in the missing operations.
	 */

	Logger log = Logger.getLogger(NodeResource.class.getCanonicalName());

	@Context
	UriInfo uriInfo;

	@Context
	HttpHeaders headers;

	@GET
	@Path("info")
	@Produces("application/xml")
	public Response getNodeInfoXML() {
		return new NodeService(headers, uriInfo).getNodeInfo();
	}

	@GET
	@Path("info")
	@Produces("application/json")
	public Response getNodeInfoJSON() {
		return new NodeService(headers, uriInfo).getNodeInfo();
	}


	@PUT
	@Path("notify")
	@Consumes("application/xml")
	@Produces("application/xml")
	/*
	 * Actually returns a TableRep (annotated with @XmlRootElement)
	 */
	public Response putNotify(TableRep predDb) {
		/*
		 * See the comment for WebClient::notify (the client side of this logic).
		 */
		return new NodeService(headers, uriInfo).notify(predDb);
		// NodeInfo p = predDb.getInfo();
	}
	
	@GET
	@Path("find")
	@Produces("application/xml")
	public Response findSuccessor(@QueryParam("id") String index) {
		int id = Integer.parseInt(index);
		return new NodeService(headers, uriInfo).findSuccessor(id);
	}
	
	@GET
	@Path("pred")
	@Produces("application/xml")
	public Response getPred() {
		return new NodeService(headers, uriInfo).getPred();
	}
	
	@GET
	@Path("succ")
	@Produces("application/xml")
	public Response getSucc() {
		return new NodeService(headers, uriInfo).getSucc();
	}
	
	@GET
	@Path("finger")
	@Produces("application/xml")
	public Response getFinger(@QueryParam("index") int index) {
		return new NodeService(headers, uriInfo).getFinger(index);
	}
	
	/*
	 * ---------------------------------------- Key Operation
	 */
	
	@GET
	@Produces("application/xml")
	public Response getKeyBinds(@QueryParam("key") String key) throws Invalid {
		return new NodeService(headers, uriInfo).get(key);
	}
	
	@PUT
	@Produces("application/xml")
	public Response putKeyBinds(@QueryParam("key") String key, @QueryParam("value") String value) throws Invalid {
		return new NodeService(headers, uriInfo).put(key, value);
	}
	
	@DELETE
	@Produces("application/xml")
	public Response deleteKeyBinds(@QueryParam("key") String key, @QueryParam("value") String value) throws Invalid {
		return new NodeService(headers, uriInfo).delete(key, value);
	}

	/*
	 * ---------------------------------------  Close Preceding Finger
	 */
	@GET
	@Path("closestfinger")
	@Produces("application/xml")
	public Response getClosestPrecedingFinger(@QueryParam("id") int id) throws Invalid {
		return new NodeService(headers, uriInfo).findClosestPrecedingFinger(id);
	}

}
