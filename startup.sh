#!/bin/bash

echo "Running <$0> script"

# API Tutorial Startup Script

# Check for reset flag
# if [ "$1" = "--reset" ]; then
#     echo "Resetting first run flag..."
#     rm -f ~/.apitutorial/first-run-complete
#     echo "First run flag reset. Next startup will recreate database."
# fi

echo "Starting API Tutorial Application..."

# Set environment variables
export PORT=8443
# export DB_URL="jdbc:mysql://localhost:3306/apitutorial?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true"
# export DB_USER="root"
# export DB_PASSWORD="password"

# Check if MySQL is running
# if ! pgrep -x "mysqld" > /dev/null; then
#     echo "MySQL is not running. Please start MySQL first."
#     exit 1
# fi

# Use DBCP2:
# java -Ddb.pool.type=DBCP2 -jar your-app.jar

# Build the application
echo "Building application..."
mvn clean compile -q

# Run the application
echo "Starting server on port $PORT..."
mvn exec:java -Dexec.mainClass="com.myapitutorial.core.App" -q

echo "Finished <$0> script"