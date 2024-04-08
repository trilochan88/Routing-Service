# Routing Service

## Overview

This project develops a routing service that effectively manages HTTP POST requests by distributing them across a set of
pre-configured nodes. Operating as a middleware, the Round Robin API seamlessly orchestrates the flow of incoming HTTP
POST requests, ensuring they are evenly distributed to various instances of the application API. This distribution
follows a round-robin strategy, which guarantees equitable request handling and resource utilization across all server
instances. Once a request is processed, the Round Robin API promptly relays the corresponding response back to the
client, maintaining a streamlined and efficient communication pipeline.

## Getting started

### Prerequisites

* Docker and docker-compose should be installed in the system
* This project is written in scala3.4.1 on jvm 19
* Install Postman on your local machine
* Import Postman Collection existed in repository root directory - **routing-service.postman_collection.json**

## Running the application

* Clone the repository
    * `git clone <repository-url> `
    * `cd RoutingService`
* Run command `docker-compose up -d` up to create routing service and three external service via wiremock
* Internal Health endpoint is running in background which call each server on health endpoint at regular interval
* Open postman collection there are two endpoints
    * Normal POST - http://localhost:8080/update-points which respond fast
    * Delay POST - http://localhost:8080/update-points-delay which simulate slow endpoint

## Testing the Application

### Scenario 1 - Round-Robin test

* `docker-compose up -d` all service will be up and running
* export postman collection from repository to postman and click on collection settings and choose `run collection`
  choose any iteration number and run
* response will be 200 OK as expected result and logs to verify round-robin functionaliy

### Scenario 2 - Health status update

* _bring down one of the wiremock service_ `ex: wiremock1`
* Follow the same steps 2 and 3 described in scenario 1, **request should not be going to down server port 9000**
* Expected result only server service will route will be 9001 and 9002

### scenario 3 - Slow response

* `docker-compose up -d` all service will be up and running
* Use **Delay POST - http://localhost:8080/update-points-delay**
* export postman collection from repository to postman and click on collection settings and choose `run collection`
  choose any iteration number and run
* Once Circuit breaker will open, it will remove the API server from request which are slow
* chose Normal POST - http://localhost:8080/update-points after 1 minute or any configuration circuit will close and
  service will start responding normally
* If we want to target specific maybeNode, we can modify wiremock endpoints

## Other part for future consideration

- Service discovery to add nodes dynamically
- adding real time monitoring and alerting via APM services like **NewRelic**, **Grafana**




