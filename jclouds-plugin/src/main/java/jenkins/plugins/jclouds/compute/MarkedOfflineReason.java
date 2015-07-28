package jenkins.plugins.jclouds.compute;

public enum MarkedOfflineReason {

    SINGLE_USE_SLAVE_BUILDWRAPPER("The job indicated that the slave was single-use"),
    SINGLE_USE_SLAVE_REAPER("The template configuration indicated the slave was single-use"),
    RETENTION_TIME_EXCEEDED("The retention time for the idle slave was exceeded");
    
    private final String message;
    
    MarkedOfflineReason(String message) {
	this.message = message;
    }
    
    public String getMessage() {
	return message;
    }
    
}
