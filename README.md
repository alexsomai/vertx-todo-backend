This project is an implementation of [Todo-Backend](http://www.todobackend.com/) using [Vert.x](http://vertx.io/)

## Run locally
To build a "fat jar" with maven:

    mvn clean package

To run the fat jar:

    java -jar target/todo-backend-1.0-SNAPSHOT-fat.jar

Alternatively, you can also run the fat jar with maven:

    mvn package exec:exec@run-app

Now point your browser at [http://localhost:8080/todos](http://localhost:8080/todos)

## OpenShift
The project is deployed on OpenShift. It can be accessed [here](http://demo-todobackend.rhcloud.com/todos).

## Todo-Backend
The application is built according to the Todo-Backend specifications.   
It passes all the [automated spec tests](http://www.todobackend.com/specs/index.html?http://demo-todobackend.rhcloud.com/todos).  
The backend may be tested using the available [client app](http://www.todobackend.com/client/index.html?http://demo-todobackend.rhcloud.com/).
