# rabbitmq-rpc-server-client

Learn RPC messaging using RabbitMQ, password encryption, timeout on remote call not returning response. </br>

Note that for the nature of RPC call, we would set auto acknowledgement of the message.  The expectation of </br>
RPC call would return a response. So, explicit achnowledgement is not needed.  Since RabbitMQ don't have a way </br>
to timeout the RPC call, we would wrap the call in task with custom timeout. </br>

The use case is to be able to execute command on the server remotely.  Look for dotnet-rabbitmq-rpc-client in my </br>
repository, it can send a command to the SpringBoot RabbitMQ server </br>

# Prerequisite

Need jdk 1.8 or above installed </br>
Need Maven 3.x installed </br>
Need to install and start RabbitMQ broker </br>

# Build at directory where pom.xml located

mvn clean install </br>

# Start Server at directory where pom.xml located

java -jar target/acme-rabbitmq-services-1.0.jar </br>

# Start Client at directory where pom.xml located

java -cp target/acme-rabbitmq-services-1.0.jar -Dloader.main=com.acme.rabbitmq.CommandSender org.springframework.boot.loader.PropertiesLauncher </br>
