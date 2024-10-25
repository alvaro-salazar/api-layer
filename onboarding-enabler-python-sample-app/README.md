# Hello World API Service in Python

This is an example about how an API service implemented in Python can be registered to the API Mediation Layer using the Python Onboarding Enabler SDK. 


 [app.py](app.py) starts the API service implemented in Python and Flask and registers it to the Discovery service using the Python Onboarding Enabler.

 This example contains the full HTTPS validation of both Discovery Service and the Hello World service.

 The certificate, private key for the service, and the local CA certificate are loaded from `keystore/localhost/localhost.keystore.p12`.
 
 ## How to run

You can start the service using by running:

 ```shell
 cd onboarding-enabler-python-sample-app
 python app.py
 ```
 
 from the root project.

If the APIML is already running then you should see the following messages:

    * Running on https://127.0.0.1:10018
    Service registered successfully.

Then you can access it via Gateway by issuing the following command:

    http --verify=../keystore/local_ca/localca.cer GET https://localhost:10010//pythonservice/api/v1/hello

## Registration to the Discovery Service

The registration is performed by calling the Python enabler library `register` method.

