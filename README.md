
# digital-platform-reporting-stubs

The Reporting Rules for Digital Platforms is a new international tax policy that has been designed by the Organisation for Economic Cooperation and Development (OECD) with the input of many tax jurisdictions around the world. The policy was designed in response to concerns many tax jurisdictions raised that the ‘Gig Economy’ may not be taxed fairly or transparently enough and presented risks to the revenue to most tax jurisdictions. This policy also sits alongside the EU’s equivalent policy called Directive of Administrative Cooperation 7 or DAC7 which was implement by EU members on January 23.

This is a simple stub service to simulate calls to EIS (IF).

## Key Terminologies
### Digital Platform Operators:
These can vary significantly in size, employee headcount, number of sellers, products/ services offered to customers and number/value of transactions. These are the stakeholders who have the legal obligation to submit data to HMRC if they fall into scope of the policy.
### Third parties:
These can be agents or service providers who will act potentially on behalf of digital platforms who are in scope of the policy. Some digital platforms may submit data themselves, others may use these third parties to act on their behalf.
### HMRC:
HMRC (via the International Data Exchange Team - IDET) will be responsible for receiving the data submitted, ensuring that users are supported in meeting their legal obligations to submit data, ensuring the data is acquired and exchanged appropriately as per the policy.
### Sellers:
Individuals, businesses or companies who are actively selling products or services to customers - referred to as the “Gig Economy” by the OECD. Although they will not use the service, it will be their data that will be submitted to HMRC by the respective digital platforms.

## Running the service

You can use service manager to run all dependent microservices using the command below

    sm2 --start DPRS_ALL


Or you could run this microservice locally using

    sm2 --stop DIGITAL_PLATFORM_REPORTING_STUBS
    sbt run

This application runs on port `20000`, by default.

## Stopping the service


You can use service manager to stop all dependent microservices using the command below

    sm2 --stop DPRS_ALL

Or you could stop this microservice locally using

    sm2 --stop DIGITAL_PLATFORM_REPORTING_STUBS

## Integration and unit tests

To run the unit tests:

    Run 'sbt test' from directory the project is stored in 

To check code coverage:

    sbt clean coverage test it/test coverageReport   
To run Integration tests:

    sbt it/test

## Dev

### Verification

Before pushing, you can run this [verification script](verify.sh):

```
./verify.sh
```

Or, to format and verify you can run the following:

``` 
./format.sh && ./verify.sh
```


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").