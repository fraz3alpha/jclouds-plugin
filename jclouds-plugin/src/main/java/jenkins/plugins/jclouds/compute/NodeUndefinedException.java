package jenkins.plugins.jclouds.compute;

public class NodeUndefinedException extends Exception {

	public NodeUndefinedException() {
		super("The NodeId has not been set. The slave is either currently being provisioned, or Jenkins was interrupted during provisioning");
	}
	
}
