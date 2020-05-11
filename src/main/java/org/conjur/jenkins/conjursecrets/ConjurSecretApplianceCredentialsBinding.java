package org.conjur.jenkins.conjursecrets;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.credentialsbinding.BindingDescriptor;
import org.jenkinsci.plugins.credentialsbinding.MultiBinding;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 *
 * @author campom10
 */
public class ConjurSecretApplianceCredentialsBinding extends MultiBinding<ConjurSecretApplianceCredentials>{

        @Symbol("ConjurSecretApplianceCredentials")
	@Extension
	public static class DescriptorImpl extends BindingDescriptor<ConjurSecretApplianceCredentials> {

		@Override
		public String getDisplayName() {
			return "Conjur Secret Credential Appliance";
		}

		@Override
		public boolean requiresWorkspace() {
			return false;
		}

		@Override
		protected Class<ConjurSecretApplianceCredentials> type() {
			return ConjurSecretApplianceCredentials.class;
		}
	}

    private static final Logger LOGGER = Logger.getLogger(ConjurSecretApplianceCredentialsBinding.class.getName());
    
    private String variable;
    private String sPath;

    @DataBoundConstructor
    public ConjurSecretApplianceCredentialsBinding(String credentialsId) {
	super(credentialsId);
//        LOGGER.log(Level.INFO, "Constructor binding");
        
    }
    
    @Override
    protected Class<ConjurSecretApplianceCredentials> type() {
//        LOGGER.log(Level.INFO, "Binding Type");
        return ConjurSecretApplianceCredentials.class;
    }

    @Override
    public MultiEnvironment bind(Run<?, ?> build, FilePath workSpace, Launcher launcher, TaskListener listener)
                    throws IOException, InterruptedException {
//             LOGGER.log(Level.INFO, "Binding Bind");
            ConjurSecretApplianceCredentials ConjurSecretApplianceCredentials = getCredentials(build);
            ConjurSecretApplianceCredentials.setContext(build);

            return new MultiEnvironment(
                            Collections.singletonMap(variable, ConjurSecretApplianceCredentials.getSecret(sPath).getPlainText()));
    }
    
    @DataBoundSetter
    public void setVariable(String variable) {
//            LOGGER.log(Level.INFO, "Setting variable to {0}", variable);
            this.variable = variable;
    }

    @Override
    public Set<String> variables() {
//        LOGGER.log(Level.INFO, "Binding Variables");
        return Collections.singleton(variable);
    }
    //@Override
    public String getsPath() {
//         LOGGER.log(Level.INFO, "BsetSpath");
        return sPath;
    }
    @DataBoundSetter
    public void setsPath(String sPath) {
//         LOGGER.log(Level.INFO, "BgetSpath");
        this.sPath = sPath;
    }    
}
