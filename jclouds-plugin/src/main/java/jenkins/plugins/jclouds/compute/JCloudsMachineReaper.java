package jenkins.plugins.jclouds.compute;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Run;
import hudson.model.listeners.RunListener;
import hudson.slaves.OfflineCause;

import java.util.logging.Logger;

import jenkins.YesNoMaybe;

/*
 * This Listener looks for the completion of jobs on a JClouds slave, and 
 *  can enforce an cloud-wide or template-wide policy on whether the slave
 *  should be one-shot. This works in tandem with the the per-job setting,
 *  but gives admins a more general option.
 */
@Extension(dynamicLoadable=YesNoMaybe.YES)
public class JCloudsMachineReaper extends RunListener<Run<?, ?>> {
	
	private Logger logger = Logger.getLogger(JCloudsMachineReaper.class.getName());
	
	public void onCompleted(Run<?, ?> r, hudson.model.TaskListener listener) {
		/*
		 * On completion of a job, we can enquire as to which slave was running the
		 *  task, and mark it as offline if required.
		 */
		logger.info("Firing onCompleted() listener for " + r + " - " + listener);
		Executor e = r.getExecutor();
		if (e != null) {
			Computer c = e.getOwner();
			if (JCloudsComputer.class.isInstance(c)) {
				AbstractJCloudsSlave jcs = ((JCloudsComputer)c).getNode();
				if (jcs.isSingleUse()) {
					if (c.isOnline()) {
						logger.info("JCloudsSlave is set as single-use, marking "+c.getName()+" offline");
						c.setTemporarilyOffline(true, OfflineCause.create(Messages._OneOffCause()));
					} else {
						logger.info("JCloudsSlave is set as single-use, but is not online, marking "+c.getName()+" offline");
					}
				} else {
					logger.info("JCloudsSlave is not single use, leaving "+c.getName()+" alone");
				}
			} else {
				logger.info("Completed job did not occur on a JClouds slave: " + c);
			}
		} else {
			logger.warning("Exectutor was null for Run, unable to determine slave removal strategy: "+r);
		}
	};

}
