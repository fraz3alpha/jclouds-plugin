package jenkins.plugins.jclouds.compute;

import static java.util.Collections.sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.logging.Level;

import hudson.Extension;
import hudson.RelativePath;
import hudson.Util;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.ItemGroup;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import jenkins.plugins.jclouds.compute.JCloudsSlaveTemplate.OverrideOpenstackOptions;

import org.apache.commons.lang.StringUtils;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.domain.Location;
import org.jclouds.predicates.validators.DnsNameValidator;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHAuthenticator;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.trilead.ssh2.Connection;

public class UserDataJCloudsSlaveTemplate extends JCloudsSlaveTemplate {

	static final String initScript = "";
	static final String userData = "";
	static final boolean preExistingJenkinsUser = false;
	static final boolean allowSudo = false; 
	static final boolean installPrivateKey = false;
	static final boolean preInstalledJava = true;
	static final String vmPassword = "";
    static final String vmUser = "";
	
	@DataBoundConstructor
	public UserDataJCloudsSlaveTemplate (final String name, final String imageId, final String imageNameRegex, final String hardwareId, final double cores,
                final int ram, final String osFamily, final String osVersion, final String locationId, final String labelString, final String description,
                final String numExecutors, final boolean stopOnTerminate, final String jvmOptions, final String fsRoot,  final int overrideRetentionTime, final int spoolDelayMs,
                final boolean assignFloatingIp, final boolean waitPhoneHome, final int waitPhoneHomeTimeout, final String keyPairName, final String availabilityZone, 
                final OverrideOpenstackOptions overrideOpenstackOptions, final boolean assignPublicIp, final String networks, final String securityGroups, final String credentialsId) {
		
		super(  name,   imageId,   imageNameRegex,   hardwareId,   cores,
                 ram,   osFamily,   osVersion,   locationId,   labelString,   description,
                  initScript,   userData,   numExecutors,   stopOnTerminate,   vmPassword,
                  vmUser,   preInstalledJava,   jvmOptions,   preExistingJenkinsUser,
                  fsRoot,   allowSudo,   installPrivateKey,  overrideRetentionTime,  spoolDelayMs,
                  assignFloatingIp,   waitPhoneHome,  waitPhoneHomeTimeout,   keyPairName,   availabilityZone, 
                 overrideOpenstackOptions,   assignPublicIp,   networks,   securityGroups,   credentialsId);
		
	}
	
    @Extension
    public static final class DescriptorImpl extends JCloudsSlaveTemplate.DescriptorImpl {

        @Override
        public String getDisplayName() {
            return "JCloudsSlaveTemplate (UserData)";
        }

    }
	
}
