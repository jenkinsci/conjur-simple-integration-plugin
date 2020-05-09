package org.conjur.jenkins.conjursecrets;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.model.Item;
import hudson.model.Run;
import hudson.security.ACL;
import hudson.util.Secret;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

/**
 *
 * @author campom10
 */
@NameWith(value = ConjurSecretApplianceCredentials.NameProvider.class, priority = 1)
public interface ConjurSecretApplianceCredentials extends StandardCredentials {
    
    	static Logger getLogger() {
		return Logger.getLogger(ConjurSecretApplianceCredentials.class.getName());
	}

	class NameProvider extends CredentialsNameProvider<ConjurSecretApplianceCredentials> {

		@Override
		public String getName(ConjurSecretApplianceCredentials c) {
			return c.getDisplayName() + c.getNameTag() + " (" + c.getDescription() + ")";
		}

	}
        
        String getDisplayName();
	String getNameTag();
//        String getName();
	Secret getSecret(String sPath);
        String getAccount();
        String getApplianceURL();
        String getcjUser();
	Secret getcjPassword();
	void setContext(Run<?, ?> context);
        

}
