package org.conjur.jenkins.conjursecrets;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Run;
import hudson.util.Secret;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import okhttp3.OkHttpClient;
import org.conjur.jenkins.api.ConjurAPI;
import org.conjur.jenkins.api.ConjurAPI.ConjurAuthnInfo;
import org.conjur.jenkins.exceptions.InvalidConjurSecretException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 *
 * @author campom10
 */
public class ConjurSecretApplianceCredentialsImpl extends BaseStandardCredentials implements ConjurSecretApplianceCredentials{
     	

        @Extension
        public static class DescriptorImpl extends ConjurSecretApplianceCredentialsDescriptor {

                @Override
                public String getDisplayName() {
                        return "Conjur Secret Credential";
                }

        }

        public static String getDescriptorDisplayName() {
                return "Conjur Secret Credential Appliance";
        }  

        private static final long serialVersionUID = 1L;

        private static final Logger LOGGER = Logger.getLogger(ConjurSecretApplianceCredentialsImpl.class.getName());

        private String cjUser;
        private Secret cjPassword;
        private String credentialID;
        private String account;
        private String applianceURL;
        private static final String authnPath ="authn";

        private String variablePath; // to be used as Username // Pending to be removed.


        private transient Run<?, ?> context;


        @DataBoundConstructor
        public ConjurSecretApplianceCredentialsImpl(@CheckForNull CredentialsScope scope, @CheckForNull String id,
                        @CheckForNull String variablePath, @CheckForNull String description) {
                super(scope, id, description);
                this.variablePath = variablePath;
        }

        public String getCredentialID() {
                LOGGER.log(Level.INFO, "Get Credentials");
                return credentialID;
        }

        @DataBoundSetter
        public void setCredentialID(String credentialID) {
                LOGGER.log(Level.INFO, "Set Credentials");
                this.credentialID = credentialID;
        }

        @Override
        public String getDisplayName() {
                 LOGGER.log(Level.INFO, "getDisplayName");
                 return "Conjur Secret Credential Appliance";
        }

        @Override
        public String getNameTag() {
            LOGGER.log(Level.INFO, "getNameTag");

            return "/*Conjur*";
        }

        public Secret getSecret(String sPath) {
            LOGGER.log(Level.INFO, "getSecret Wpath");
                    String result = "";
                    try {
                            // initiate conjurAuthn //not sure if can be done in constructor. or via setter/getters.
                            ConjurAuthnInfo conjurAuthn = new ConjurAuthnInfo(applianceURL ,authnPath ,account ,cjUser ,cjPassword.getPlainText());
                            // Get Http Client
                            OkHttpClient client = ConjurAPI.getHttpClient();
    /*                        // Debug auth creation
                            LOGGER.log(Level.INFO, "getSecret: applianceURL:" + applianceURL);
                            LOGGER.log(Level.INFO, "getSecret: authnPath:" + authnPath);
                            LOGGER.log(Level.INFO, "getSecret: account:" + account);
                            LOGGER.log(Level.INFO, "getSecret: cjUser:" + cjUser);
                            LOGGER.log(Level.INFO, "getSecret: cjPassword:" + cjPassword.getPlainText());
    */
                            // Authenticate to Conjur
                            String authToken = ConjurAPI.getAuthorizationToken(client, conjurAuthn, context);

                            // Retrieve secret from Conjur
                            String secretString = ConjurAPI.getSecret(client, conjurAuthn, authToken, sPath);
                            result = secretString;
                    } catch (IOException e) {
                            LOGGER.log(Level.WARNING, "EXCEPTION: " + e.getMessage());
                            throw new InvalidConjurSecretException(e.getMessage(), e);
                    }
                    LOGGER.log(Level.INFO, "Top Secret: "+ result);
                    return Secret.fromString(result);
        }

    /*    @Override
        public void setConjurConfiguration(ConjurConfiguration conjurConfiguration) {
            LOGGER.log(Level.INFO, "setconjurConfitguraion");
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    */
        @Override
        public void setContext(Run<?, ?> context) {
            LOGGER.log(Level.INFO, "Set Context");
            if (context != null)
                    this.context = context;
        }
        @Override
        public String getAccount() {
            LOGGER.log(Level.INFO, "getAccount");
            return account;
        }

        @DataBoundSetter
        public void setAccount(String account) {
            LOGGER.log(Level.INFO, "setAccount");
            this.account = account;
        }
        @Override
        public String getApplianceURL() {
            LOGGER.log(Level.INFO, "getApplianceUrl");
            return applianceURL;
        }
        @DataBoundSetter
        public void setApplianceURL(String applianceURL) {
            LOGGER.log(Level.INFO, "SetApplianceUrl");
            this.applianceURL = applianceURL;
        }
        @Override
        public String getcjUser() {
            return cjUser;
        }
        @DataBoundSetter
        public void setcjUser(String cjUser) {
            this.cjUser = cjUser;
        }
        @Override
        public Secret getcjPassword() {
            LOGGER.log(Level.INFO, "getPassword");
            return cjPassword;
        }
        @DataBoundSetter
        public void setcjPassword(Secret cjPassword) {
            LOGGER.log(Level.INFO, "SetPassword");
            this.cjPassword = cjPassword;
        }

/*        @Override
        public String getName() {
            return "ConjurSecretApplianceCredentialsImpl";
        }
*/

}
