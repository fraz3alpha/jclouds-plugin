package jenkins.plugins.jclouds.compute;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.Descriptor;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.util.FormValidation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import jenkins.model.Jenkins;
import jenkins.plugins.jclouds.compute.JCloudsSlaveTemplate.DescriptorImpl;
import jenkins.plugins.jclouds.compute.JCloudsSlaveTemplate.OverrideOpenstackOptions;

import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.options.TemplateOptions;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import shaded.com.google.common.collect.ImmutableMap;

public class JnlpJCloudsSlaveTemplate extends JCloudsSlaveTemplate {
	
    private static final Logger LOGGER = Logger.getLogger(JnlpJCloudsSlaveTemplate.class.getName());
    
    public static final String USER_DATA_LOAD_FAILURE = "ERROR: Unable to load default user data template";

    // For reference, these are what we no longer need to set in the super class
	static final String NO_OP_INITSCRIPT = "";
	static final String NO_OP_USERDATA = "";
	static final boolean NO_OP_PREEXISTING_JENKINS_USER = true;
	static final boolean NO_OP_ALLOW_SUDO = false; 
	static final boolean NO_OP_INSTALL_PRIVATE_KEY = false;
	static final boolean NO_OP_PREINSTALLED_JAVA = true;
	static final String NO_OP_VM_PASSWORD = "";
    static final String NO_OP_VM_USER = "";
    static final boolean NO_OP_WAIT_PHONE_HOME = false;
    static final int NO_OP_WAIT_PHONE_HOME_TIMEOUT = 0; 
    
    public static final String PLACEHOLDER_SECRET = "@@@secret@@@";
    
    private final String userDataTemplate;
	
	@DataBoundConstructor
	public JnlpJCloudsSlaveTemplate (final String name, final String imageId, final String imageNameRegex, final String hardwareId, final double cores,
                final int ram, final String osFamily, final String osVersion, final String locationId, final String labelString, final String description,
                final String numExecutors, final boolean stopOnTerminate, final String jvmOptions, final String userDataTemplate, final String fsRoot,  
                final int overrideRetentionTime, final int spoolDelayMs, final boolean assignFloatingIp, final String keyPairName, final String availabilityZone, 
                final OverrideOpenstackOptions overrideOpenstackOptions, final boolean assignPublicIp, final String networks, final String securityGroups, final String credentialsId) {
		
		super(  name,   imageId,   imageNameRegex,   hardwareId,   cores,
                 ram,   osFamily,   osVersion,   locationId,   labelString,   description,
                  NO_OP_INITSCRIPT,   NO_OP_USERDATA,   numExecutors,   stopOnTerminate,   NO_OP_VM_PASSWORD,
                  NO_OP_VM_USER,   NO_OP_PREINSTALLED_JAVA,   jvmOptions,   NO_OP_PREEXISTING_JENKINS_USER,
                  fsRoot,   NO_OP_ALLOW_SUDO,   NO_OP_INSTALL_PRIVATE_KEY,  overrideRetentionTime,  spoolDelayMs,
                  assignFloatingIp,   NO_OP_WAIT_PHONE_HOME,  NO_OP_WAIT_PHONE_HOME_TIMEOUT,   keyPairName,   availabilityZone, 
                 overrideOpenstackOptions,   assignPublicIp,   networks,   securityGroups,   credentialsId);
		
		LOGGER.info("Instantiating JnlpJCloudsSlaveTemplate");
		
		this.userDataTemplate = userDataTemplate;
		
		if (this.credentialsId == null) {
			throw new IllegalArgumentException("Credentials need to be supplied for the JNLP Slave Template");
		}
		// There must be another method to look these up, but the SSHLauncher has a convenient method that
		//  works for all credential types
        StandardUsernameCredentials credentials = SSHLauncher.lookupSystemCredentials(this.credentialsId);
        if (credentials == null || ! (credentials instanceof StandardUsernamePasswordCredentials)) {
        	throw new IllegalArgumentException("The credentials supplied are not of the correct form, they must be a Username & Password combination");
        }
		
	}
   
	
	@Override
    public AbstractJCloudsSlave provisionSlave(TaskListener listener) throws IOException {

		LOGGER.info("Provisioning new JNLP jclouds node");
		JnlpJCloudsSlave s = null;
        try {
        	
        	// Rate limit provisioning, if required
        	throttle();
        	
        	String instanceName = name + "-" + UUID.randomUUID().toString();
        	
            s = new JnlpJCloudsSlave(getCloud().getDisplayName(), instanceName, getFsRoot(), labelString, description,
                    numExecutors, stopOnTerminate, overrideRetentionTime);

            // Add node to Jenkins (needed for the provisioned machine to dial in to)
            Jenkins.getInstance().addNode(s);

            // Define image parameters and setup image template
            Template template = setupBaseTemplate(); 
            TemplateOptions options = template.getOptions();

            // Configure additional cloud parameters
            setupAddtionalProviderOptions(options);

            // Configure user data
            // User data (if supported by the provider), may run before or after the init script, depending on when the
            //  network becomes available, and an SSH connection is established. 
            setupUserData(options, getUserDataString(s));

            // Configure VM instance parameters, used in provisioning
            ImmutableMap<String, String> userMetadata = ImmutableMap.of("Name", instanceName);
           
            try {
            	// Provision machine via JClouds, blocks until the hypervisor claims it is complete
            	NodeMetadata nodeMetadata = provision(template, userMetadata);

            	// Update JCloudsSlave nodeMetadata
            	s.setNodeId(nodeMetadata.getId());
            	
            	return s;
            } catch (Throwable t) {
            	// Provision failure. Clean up at the Jenkins end
            	Jenkins.getInstance().removeNode(s);

            	// Trigger upstread cleanup or retry
            	throw t;
            }

            
        } catch (Descriptor.FormException e) {
            throw new AssertionError("Invalid configuration " + e.getMessage());
        }
    }


	@Override
    // Called by the Job BuildWrapper for spawning additional machines for a particular job run.
    // These are not attached to Jenkins as slaves
	public NodeMetadata get() {
		
        // Define image parameters and setup image template
        Template template = setupBaseTemplate(); 
        TemplateOptions options = template.getOptions();

        // Configure additional cloud parameters
        setupAddtionalProviderOptions(options);

        // Configure VM instance parameters, used in provisioning
        ImmutableMap<String, String> userMetadata = ImmutableMap.of("Name", name);
        
        return provision(template, userMetadata);
	}
		
	/**
	 * 
	 * @return The user_data string to dial a Jenkins Agent in from the newly provisioned box
	 */
	private String getUserDataString(JnlpJCloudsSlave s) {
		
		String userdata = getUserDataTemplate();

		// Get the secret from the created slave, needed for the user_data section
        String secret = s.getComputer().getJnlpMac();
        
        // Get the computer name, needed for the user_data section
        String slave_id = s.getComputer().getDisplayName();
        
        // The Jenkins URL, needed to download files
        String jenkins_server = Jenkins.getInstance().getRootUrl();
        
        String jenkins_dir = s.getRemoteFS();
             
        String jenkins_user = null;
        String jenkins_password = null;
        
		// There must be another method to look these up, but the SSHLauncher has a convenient method that
		//  works for all credential types
        StandardUsernameCredentials credentials = SSHLauncher.lookupSystemCredentials(credentialsId);
        if (credentials != null && credentials instanceof StandardUsernamePasswordCredentials) {
        	jenkins_user = credentials.getUsername();
        	jenkins_password = ((StandardUsernamePasswordCredentials) credentials).getPassword().getPlainText();
        } else {
        	throw new IllegalArgumentException("The credentials supplied are not of the correct form, they must be a Username & Password combination");
        }
        
        /*
         * Variables to replace, bookended by @@@
         * @@@_@@@
         * jenkins_server
         * jenkins_dir
         * jenkins_user
         * jenkins_password
         * slave_id
         * java
         * secret
         */
        
        userdata = userdata.replaceAll("@@@jenkins_server@@@", Matcher.quoteReplacement(jenkins_server));
        userdata = userdata.replaceAll("@@@jenkins_dir@@@", Matcher.quoteReplacement(jenkins_dir));
        userdata = userdata.replaceAll("@@@jenkins_user@@@", Matcher.quoteReplacement(jenkins_user));
        userdata = userdata.replaceAll("@@@jenkins_password@@@", Matcher.quoteReplacement(jenkins_password));
        userdata = userdata.replaceAll("@@@slave_id@@@", Matcher.quoteReplacement(slave_id));
        userdata = userdata.replaceAll("@@@java@@@", Matcher.quoteReplacement("java.exe"));
        userdata = userdata.replaceAll(PLACEHOLDER_SECRET, Matcher.quoteReplacement(secret));

        // TODO: Get additional JVM arguments to be passed to the slave
        // userdata.replaceAll("@@@extra_jvm_options@@@", );
        
        // TODO: Need to remove the password from anything we print out
        LOGGER.info("Provisioning slave with userdata: " + userdata);
        
		return userdata;
	}
	
	/**
	 * Public getter to populate the configuration UI
	 * @return
	 */
	public String getUserDataTemplate() {
		LOGGER.info("getUserDataTemplate()");
        if (userDataTemplate == null || userDataTemplate.isEmpty()) {
        	LOGGER.info("Fetching default template");
//            return getDefaultWindowsUserDataTemplate();
            return "";
        } else {
        	LOGGER.info("Returning existing template (" + userDataTemplate.length() + " bytes)");
            return userDataTemplate;
        }
	}
	
    @Extension
    public static class DescriptorImpl extends JCloudsSlaveTemplate.DescriptorImpl {

        @Override
        public String getDisplayName() {
            return "JCloudsSlaveTemplate (JNLP)";
        }
        
        public FormValidation doCheckUserDataTemplate(@QueryParameter String value) {
        	if (value == null || value.trim().isEmpty()) {
        		return FormValidation.error("User Data Template must not be empty");
        	}
           	if (value.contains(USER_DATA_LOAD_FAILURE)) {
           		return FormValidation.error("User Data Template could not be loaded, and has not been replaced with a valid script");
           	}
           	if (!value.contains(PLACEHOLDER_SECRET)) {
           		return FormValidation.error("User Data Template does not contain " + PLACEHOLDER_SECRET + 
           				", which will be required to dial in a slave");
           	}
           	return FormValidation.ok();
        }
        
    	/**
    	 * 
    	 * @return The default Windows user_data string, with placeholders.
    	 */
    	public String getDefaultUserDataTemplate() {
    		
    		LOGGER.info("Getting Default Windows User Data Template from windows.tokens.ps1");
    		
    		InputStream is = this.getClass().getResourceAsStream("windows.tokens.ps1");
    		
    		String userdataString = "";
    		BufferedReader b = null;
    		try {
    			b = new BufferedReader(new InputStreamReader(is));
    			String line = b.readLine();
    			while (line != null) {
    				userdataString += line + "\r\n";
    				line = b.readLine();
    			}
    		} catch (IOException ioe) {
    			return USER_DATA_LOAD_FAILURE;
    		} finally {
    			try {
    				b.close();
    			} catch (IOException e) {
    			}
    		}
    		
    		return userdataString;
    	}

    }
	
}
