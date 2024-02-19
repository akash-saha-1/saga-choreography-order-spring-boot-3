# saga-choreography-order-spring-boot-3
### This is a spring boot 3 microservices application using saga choreography pattern. Where there are 4 microservices called order, payment, stock, delivery.

### if any of the microservices failed then it will publish the roll back event message and the previous service will consume that message and will mark transaction as failed.

### the first api call be marked as failed if any of the service failed and also unitl all the services are resolved using last microservice api call, it wont serve any respons to ui. and will calll that api in loop in every 2 seconds.

### all are publishing messages to kafka and consumers are taking the event from there and starting their own service.