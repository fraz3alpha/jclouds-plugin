package jenkins.plugins.jclouds.compute;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.Descriptor;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.NodeProperty;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.RetentionStrategy;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.domain.LoginCredentials;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Jenkins Slave node - managed by JClouds.
 *
 * @author Vijay Kiran
 */
public abstract class AbstractJCloudsSlave extends AbstractCloudSlave {
    private static final Logger LOGGER = Logger.getLogger(AbstractJCloudsSlave.class.getName());
    protected transient NodeMetadata nodeMetaData;
    public final boolean stopOnTerminate;
    protected final String cloudName;
    protected String nodeId;
    protected boolean pendingDelete;
    protected final int overrideRetentionTime;
    
    protected ComputerLauncher computerLauncher = null;

    @SuppressWarnings("rawtypes")
    public AbstractJCloudsSlave(String cloudName, String name, String nodeDescription, String remoteFS, String numExecutors, Mode mode, String labelString,
                        ComputerLauncher launcher, RetentionStrategy retentionStrategy, List<? extends NodeProperty<?>> nodeProperties, boolean stopOnTerminate,
                        int overrideRetentionTime) throws Descriptor.FormException,
            IOException {
        super(name, nodeDescription, remoteFS, numExecutors, mode, labelString, launcher, retentionStrategy, nodeProperties);
        this.stopOnTerminate = stopOnTerminate;
        this.cloudName = cloudName;
        this.overrideRetentionTime = overrideRetentionTime;
    }

    /**
     * Get Jclouds NodeMetadata associated with this Slave.
     *
     * @return {@link NodeMetadata}
     */
    public NodeMetadata getNodeMetaData() throws NodeUndefinedException {
        if (this.nodeMetaData == null) {
        	if (nodeId == null) {
        		throw new NodeUndefinedException();
        	}
            final ComputeService compute = JCloudsCloud.getByName(cloudName).getCompute();
            this.nodeMetaData = compute.getNodeMetadata(nodeId);
        }
        return nodeMetaData;
    }
    
    /**
     * Provide a mechanism to set the nodeId after instantiation to allow for different
     *  ordering of provisioning and Jenkins slave object creation
     * @param nodeId The String ID of the instance in the cloud provider
     */
    public void setNodeId(String nodeId) {
    	this.nodeId = nodeId;
    }

    /**
     * Get the retention time for this slave, defaulting to the parent cloud's if not set.
     *
     * @return overrideTime
     */
    public int getRetentionTime() {
        if (overrideRetentionTime > 0) {
            return overrideRetentionTime;
        } else {
            return JCloudsCloud.getByName(cloudName).getRetentionTime();
        }
    }

    /**
     * Get the JClouds profile identifier for the Cloud associated with this slave.
     *
     * @return cloudName
     */
    public String getCloudName() {
        return cloudName;
    }

    public boolean isPendingDelete() {
        return pendingDelete;
    }

    public void setPendingDelete(boolean pendingDelete) {
        this.pendingDelete = pendingDelete;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractCloudComputer<AbstractJCloudsSlave> createComputer() {
        LOGGER.info("Creating a new JClouds Slave");
        return new JCloudsComputer(this);
    }

    @Extension
    public static final class JCloudsSlaveDescriptor extends SlaveDescriptor {

        @Override
        public String getDisplayName() {
            return "Simple JClouds Slave";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isInstantiable() {
            return false;
        }
    }

    /**
     * Destroy the node calls {@link ComputeService#destroyNode}
     */
    @Override
    protected void _terminate(TaskListener listener) throws IOException, InterruptedException {
    	if (nodeId == null) {
    		throw new IOException(new NodeUndefinedException());
    	}
        final ComputeService compute = JCloudsCloud.getByName(cloudName).getCompute();
        if (compute.getNodeMetadata(nodeId) != null && compute.getNodeMetadata(nodeId).getStatus().equals(NodeMetadata.Status.RUNNING)) {
            if (stopOnTerminate) {
                LOGGER.info("Suspending the Slave : " + getNodeName());
                compute.suspendNode(nodeId);
            } else {
                LOGGER.info("Terminating the Slave : " + getNodeName());
                compute.destroyNode(nodeId);
            }
        } else {
            LOGGER.info("Slave " + getNodeName() + " is already not running.");
        }
    }
    
}
