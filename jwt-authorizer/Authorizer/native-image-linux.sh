PROJECT_NAME=Authorizer
PROJECT_VERSION=1.0.0

# Generate Jar file
sudo rm -rf target;
mvn clean install;

# Generate Native Image
docker run --rm --name graal -v $(pwd):/${PROJECT_NAME} springci/graalvm-ce:master-java11 \
    /bin/bash -c "native-image \
                    -H:EnableURLProtocols=http \
		                -H:ReflectionConfigurationFiles=/${PROJECT_NAME}/reflect.json \
                    -jar /${PROJECT_NAME}/target/${PROJECT_NAME}-${PROJECT_VERSION}.jar \
                    ; \
                    mkdir /${PROJECT_NAME}/target/custom-runtime \
                    ; \
                    cp ${PROJECT_NAME}-${PROJECT_VERSION} /${PROJECT_NAME}/target/custom-runtime/${PROJECT_NAME}";

# Copy the bootstrap to the custom-runtime
sudo cp bootstrap target/custom-runtime/bootstrap;

# Make bootstrap executable
chmod +x target/custom-runtime/bootstrap;

# Zip
rm $PROJECT_NAME-custom-runtime.zip
cd target/custom-runtime || exit
zip -X -r ../../$PROJECT_NAME-custom-runtime.zip .
