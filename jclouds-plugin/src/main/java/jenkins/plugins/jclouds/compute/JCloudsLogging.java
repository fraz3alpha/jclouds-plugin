package jenkins.plugins.jclouds.compute;

import hudson.model.labels.LabelAtom;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import jenkins.plugins.jclouds.compute.JCloudsCloud.OfflineTrigger;
import jenkins.plugins.jclouds.compute.JCloudsCloud.ProvisionState;
import jenkins.plugins.jclouds.compute.JCloudsCloud.ProvisionType;

import com.google.gson.Gson;

public class JCloudsLogging {
    
    private static final Logger LOGGER = Logger.getLogger(JCloudsLogging.class.getName());
    public static final String LOG_PREAMBLE = "JCloudsStatusJSON";
    
    private static final String SLAVE_CREATE_STARTED = "SLAVE_CREATE_STARTED";
    private static final String SLAVE_PROVISION_STARTED = "SLAVE_PROVISION_STARTED"; 
    private static final String SLAVE_PROVISION_FINISHED = "SLAVE_PROVISION_FINISHED";
    private static final String SLAVE_CREATE_FINISHED = "SLAVE_CREATE_FINISHED";
    private static final String SLAVE_MARKED_OFFLINE = "SLAVE_MARKED_OFFLINE";
    private static final String SLAVE_DELETE_STARTED = "SLAVE_DELETE_STARTED";
    private static final String SLAVE_DELETE_FINISHED = "SLAVE_DELETE_FINISHED";
    
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILURE = "FAILURE";
    
    private static final String LABEL_PROVISION_TYPE = "ProvisionType";
    private static final String LABEL_EVENT = "Event";
    private static final String LABEL_TIMESTAMP = "Timestamp";
    private static final String LABEL_ELAPSEDTIME_MS = "ElapsedTimeMillis";
    private static final String LABEL_STATE = "State";
    private static final String LABEL_REASON = "Reason";
    private static final String LABEL_OFFLINE_TRIGGER = "OfflineTrigger";
      
         
    public static void slaveCreateStarted(ProvisionType type, JCloudsSlaveTemplate template) {
	Map<String, Object> p = new HashMap<String, Object>();
	p.put(LABEL_EVENT, SLAVE_CREATE_STARTED);
	p.put(LABEL_PROVISION_TYPE, type);
	p.put(LABEL_ELAPSEDTIME_MS, Long.valueOf(0));
	
	if (template != null) {
	    p.putAll(template.getInfo());
	    if (template.cloud != null) {
		p.putAll(template.cloud.getInfo());
	    }
	}
	
	log(p);
    }
    
    public static void slaveCreateFinished(ProvisionType type, JCloudsSlaveTemplate template, AbstractJCloudsSlave slave, long elapsedTime) {
	Map<String, Object> p = new HashMap<String, Object>();
	p.put(LABEL_EVENT, SLAVE_CREATE_FINISHED);
	p.put(LABEL_PROVISION_TYPE, type);
	p.put(LABEL_STATE, SUCCESS);
	p.put(LABEL_ELAPSEDTIME_MS, Long.valueOf(elapsedTime));
	
	if (template != null) {
	    p.putAll(template.getInfo());
	    if (template.cloud != null) {
		p.putAll(template.cloud.getInfo());
	    }
	}

	if (slave != null) {
	    p.putAll(slave.getInfo());
	}
	
	log(p);
    }

    public static void slaveCreateFinished(ProvisionType type, JCloudsSlaveTemplate template, AbstractJCloudsSlave slave, long elapsedTime, Throwable t) {
	Map<String, Object> p = new HashMap<String, Object>();
	p.put(LABEL_EVENT, SLAVE_CREATE_FINISHED);
	p.put(LABEL_PROVISION_TYPE, type);
	p.put(LABEL_STATE, FAILURE);
	p.put(LABEL_ELAPSEDTIME_MS, Long.valueOf(elapsedTime));
	p.put(LABEL_REASON, t.getMessage());
	
	if (template != null) {
	    p.putAll(template.getInfo());
	    if (template.cloud != null) {
		p.putAll(template.cloud.getInfo());
	    }
	}

	if (slave != null) {
	    p.putAll(slave.getInfo());
	}
	
	log(p);
    }
    
    
    public static void slaveProvisionStarted(JCloudsSlaveTemplate template, long elapsedTime) {
	Map<String, Object> p = new HashMap<String, Object>();
	p.put(LABEL_EVENT, SLAVE_PROVISION_STARTED);
	p.put(LABEL_ELAPSEDTIME_MS, Long.valueOf(elapsedTime));
	
	if (template != null) {
	    p.putAll(template.getInfo());
	    if (template.cloud != null) {
		p.putAll(template.cloud.getInfo());
	    }
	}
	
	log(p);
    }

    public static void slaveProvisionFinished(JCloudsSlaveTemplate template, AbstractJCloudsSlave slave, long elapsedTime) {
	Map<String, Object> p = new HashMap<String, Object>();
	p.put(LABEL_EVENT, SLAVE_PROVISION_FINISHED);
	p.put(LABEL_STATE, SUCCESS);
	p.put(LABEL_ELAPSEDTIME_MS, Long.valueOf(elapsedTime));
	
	if (template != null) {
	    p.putAll(template.getInfo());
	    if (template.cloud != null) {
		p.putAll(template.cloud.getInfo());
	    }
	}

	if (slave != null) {
	    p.putAll(slave.getInfo());
	}
	
	log(p);
    }

    public static void slaveProvisionFinished(JCloudsSlaveTemplate template, AbstractJCloudsSlave slave, long elapsedTime, Throwable t) {
	Map<String, Object> p = new HashMap<String, Object>();
	p.put(LABEL_EVENT, SLAVE_PROVISION_FINISHED);
	p.put(LABEL_STATE, FAILURE);
	p.put(LABEL_REASON, t.getMessage());
	p.put(LABEL_ELAPSEDTIME_MS, Long.valueOf(elapsedTime));

	if (template != null) {
	    p.putAll(template.getInfo());
	    if (template.cloud != null) {
		p.putAll(template.cloud.getInfo());
	    }
	}

	if (slave != null) {
	    p.putAll(slave.getInfo());
	}
	
	log(p);
    }
    
    public static void slaveMarkedOffline(AbstractJCloudsSlave slave, OfflineTrigger cause) {
	Map<String, Object> p = new HashMap<String, Object>();
	p.put(LABEL_EVENT, SLAVE_MARKED_OFFLINE);
	p.put(LABEL_OFFLINE_TRIGGER, cause);
	
	if (slave != null) {
	    p.putAll(slave.getInfo());
	}
	
	log(p);
    }
    
    public static void slaveDeleteStarted(AbstractJCloudsSlave slave) {
	Map<String, Object> p = new HashMap<String, Object>();
	p.put(LABEL_EVENT, SLAVE_DELETE_STARTED);
	p.put(LABEL_ELAPSEDTIME_MS, Long.valueOf(0));
	
	if (slave != null) {
	    p.putAll(slave.getInfo());
	}
	
	log(p);
    }
    
    public static void slaveDeleteFinished(AbstractJCloudsSlave slave, long elapsedTime) {
	Map<String, Object> p = new HashMap<String, Object>();
	p.put(LABEL_EVENT, SLAVE_DELETE_FINISHED);
	p.put(LABEL_ELAPSEDTIME_MS, Long.valueOf(elapsedTime));
	p.put(LABEL_STATE, SUCCESS);
	
	if (slave != null) {
	    p.putAll(slave.getInfo());
	}
	
	log(p);
    }
    
    public static void slaveDeleteFinished(AbstractJCloudsSlave slave, long elapsedTime, Throwable t) {
	Map<String, Object> p = new HashMap<String, Object>();
	p.put(LABEL_EVENT, SLAVE_DELETE_FINISHED);
	p.put(LABEL_ELAPSEDTIME_MS, Long.valueOf(elapsedTime));
	p.put(LABEL_STATE, FAILURE);
	p.put(LABEL_REASON, t.getMessage());
	
	if (slave != null) {
	    p.putAll(slave.getInfo());
	}
	
	log(p);
    }
    
    private static void log(Map<String, Object> p) {
	Gson gson = new Gson();
	
	p.put(LABEL_TIMESTAMP, Long.valueOf(System.currentTimeMillis()));
	
        LOGGER.info(LOG_PREAMBLE + ": " + gson.toJson(p));
    }
    
}
