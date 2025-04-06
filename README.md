# Stock VWAP Calculator for RSU

A Java application that calculates the Volume Weighted Average Price (VWAP) for stocks using Yahoo Finance data. This project was generated and developed using [Cursor](https://cursor.sh), an AI-powered IDE.

## Features

- Fetches historical stock data from Yahoo Finance
- Calculates monthly VWAP (Volume Weighted Average Price)
- Displays trading volume statistics
- Dockerized for easy deployment
- Built with Java 17 and Maven

## Prerequisites

- Java 17 or higher
- Maven 3.8.4 or higher
- Docker (optional, for containerized deployment)

## Building and Running

### Using Maven directly:

```bash
# Build the project
mvn clean package

# Run the application
java -jar target/stock-vwap-1.0-SNAPSHOT.jar
```

### Using Docker:

```bash
# Build the Docker image
docker build -t java-docker-app .

# Run the container
docker run java-docker-app
```

## Output Format

The application displays a table with the following information for each month:
- Month (YYYY-MM format)
- VWAP (Volume Weighted Average Price)
- Total Trading Volume

Example output:
```
Monthly Volume Weighted Average Price (VWAP) for DDOG
=================================================
Month      VWAP            Total Volume   
-------------------------------------------------
2024-04    $126.43              57,570,400
2024-05    $118.03             102,429,600
...
```

## Technical Details

- Uses Yahoo Finance's v8 API endpoint
- Implements browser-like headers to prevent rate limiting
- Handles JSON response parsing with Jackson
- Includes error handling and data validation
- Uses OkHttp for HTTP requests with configurable timeouts

## Project Structure

```
.
├── Dockerfile
├── pom.xml
├── README.md
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    └── App.java
```

## Dependencies

- Jackson (JSON processing)
- OkHttp (HTTP client)
- Maven (build tool)