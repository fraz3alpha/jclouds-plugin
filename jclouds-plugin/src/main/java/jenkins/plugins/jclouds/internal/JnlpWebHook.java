package jenkins.plugins.jclouds.internal;

import hudson.Extension;
import hudson.model.RootAction;
import hudson.model.UnprotectedRootAction;
import hudson.model.Hudson;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Serves JNLP windows service files
 *  - jenkins-slave.exe
 *  - jenkins-slave.exe.config
 *
 */
@Extension
public class JnlpWebHook implements UnprotectedRootAction {
	
	public static final String URLNAME = "jclouds-jnlp";
	private static final Logger LOGGER = Logger.getLogger(JnlpWebHook.class.getName());

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return URLNAME;
    }

    /**
     * Receives the webhook call.
     */
    public void doIndex(StaplerRequest req, StaplerResponse rsp) {

        String requiredFile = req.getParameter("file");
        if (null == requiredFile) {
            throw new IllegalArgumentException("The file= parameter is required");
        }
        LOGGER.info("Received Request for " + requiredFile);

        try {
//        	URL url = getClass().getResource("/windows-service/jenkins.exe");
//        	rsp.serveFile(req, url);
        	
        	String path = null;
        	
        	if (requiredFile.equalsIgnoreCase("jenkins-slave.exe.config")) {
        		path = "/windows-service/jenkins.exe.config";
        	} else if (requiredFile.equalsIgnoreCase("jenkins-slave.exe")) {
        		path = "/windows-service/jenkins.exe";
        	} else if (requiredFile.equalsIgnoreCase("jenkins-slave.xml")) {
        		// TODO, serve the XML file
        		path = "JnlpWebHook/jenkins-slave.xml";
        	}
        	
        	if (path != null) {
        		LOGGER.info("Serving up " + requiredFile + " from " + path);
            	URL url = getClass().getResource(path);
            	rsp.serveFile(req, url);
        	} else {
        		LOGGER.info("I can't supply '"+requiredFile+"'");
        	}
        	
		} catch (ServletException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }

    public static JnlpWebHook get() {
        return Hudson.getInstance().getExtensionList(RootAction.class).get(JnlpWebHook.class);
    }

}
