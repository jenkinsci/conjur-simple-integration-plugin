# Jenkins ConjurSimpleIntegration Plugin

## Scope
This Plugin was created to enable access to Conjur Stored Secrets in a simple and convinient way.
It lacks functionality but it does what it does in an easy manner.
It allows you to create a credential to specify Conjur connection details and you can specify wich secret you can retrieve dynamically at run time.
## Configuration
For Configuration you only need to create the credential.
You must select Conjure Secret Credential.

![Credential Sample][src/main/resources/createcedential.png]


**Account**: This is your Account created in Conjur the conjur admin can provide you with this data.


**Appliance URL**: Is the url for your appliance including the "https://" part.


**Username/Host**: Here you must specify the username or host you were provided to access to your Secrets. Be aware that you should use the following format example depending if you are using USER or a HOST: (Do not include the "USER:" or "HOST:")


	**USER**:`usr@pipu.kk.com`


	**HOST**:`host/projects/project012/test/hosts/host1`


**Password**: This is the APIKEY for your user/Host.


**ID**: As in any Jenkins Credential this is the ID you will be refering in the code.


**Description**: This is a description to help you identify this Credential.

## Usage Sample
This is a short pipeline example on how to use this in a Pipeline.
As you can see you have to specify the **sPath** parameter to define de Real Secret to be retrieved.

    pipeline{
    agent any
        stages {
            stage("Testing Conjur Simple Credential)") {
                steps{
                    withCredentials([[$class: 'ConjurSecretApplianceCredentialsBinding'
                                        , credentialsId: 'MyCredential'
                                        ,sPath: 'projects/project012/test/variables/variable1'
                                        ,variable: 'SECRETO2']
                                    ]) {
                    echo "Test no dumping of credential: ${env.SECRETO2}"
                    }//withcredentials
                }// steps
            } //Stage
        }
    }// pipeline


