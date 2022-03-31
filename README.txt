# Restaurant Service

cd Restaurant

### Compile the project and generate the JAR file
./mvnw package

### Build the docker image
docker build -t restaurant-service .

### Run the docker image
docker run -p 8080:8080 --rm --name restaurant --add-host=host.docker.internal:host-gateway -v ~/Downloads/initialData.txt:/initialData.txt restaurant-service
# Notes about the -add-host option above:
# 1. This option is not explicitly required with Windows and Mac. It is
#    explicitly required with Linux.
# 2. Each Spring service can contact http://host.docker.internal:8080,
#    http://host.docker.internal:8081,  and http://host.docker.internal:8082,
#    respectively, to reach the three services, respectively. The way it
#    works is that at runtime Docker replaces "host.docker.internal" with the
#    IP address of the localhost that started all the containers.
# Separately, the -p commands forward localhost:8080 to this container.

### Stop the container
docker stop restaurant

### Remove the docker image
docker image rm restaurant-service


# Delivery Service

cd ../Delivery

### Compile and run the Akka program
mvn compile
mvn exec:java -Dexec.args=”./initialData.txt”


# Wallet Service

cd ../Wallet

### Compile the project and generate the JAR file
./mvnw package

### Build the docker image
docker build -t wallet-service .

### Build the docker image
docker run -p 8082:8080 --rm --name wallet --add-host=host.docker.internal:host-gateway -v ~/Downloads/initialData.txt:/initialData.txt wallet-service

### Stop the container
docker stop wallet

### Remove the docker image
docker image rm wallet-service

