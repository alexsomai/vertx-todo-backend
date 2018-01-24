This project is an implementation of [Todo-Backend](http://www.todobackend.com/) using [Vert.x](http://vertx.io/) and [MongoDB](https://www.mongodb.org/).

## Install and run locally
 - Clone project `git clone git@github.com:alexsomai/vertx-todo-backend.git`  
 - Install and start [MongoDB](https://www.mongodb.org/)  
 - Build the project and start application `mvn clean package exec:exec@run-app`  
 - Point your browser at [http://localhost:8080/todos](http://localhost:8080/todos)  

## Todo-Backend
The application is built according to the Todo-Backend specifications.   
It passes all the [automated spec tests](http://www.todobackend.com/specs/index.html?https://demo-todobackend-vertx.herokuapp.com/todos).  
The backend may be tested using the available [client app](http://www.todobackend.com/client/index.html?https://demo-todobackend-vertx.herokuapp.com/todos).

## OpenShift
The project is deployed on [Heroku](https://heroku.com). It can be accessed [here](https://demo-todobackend-vertx.herokuapp.com/todos).
