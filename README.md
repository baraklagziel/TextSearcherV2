# Text Searcher Project Specification

## Overview
The Text Searcher project is a Java-based application designed to efficiently locate specific strings within a large body of text. This document outlines the requirements and design specifications for the Text Searcher project.

## Objectives
The primary objective of this project is to develop a Java program capable of identifying the occurrence of predetermined strings within a large text file. The application will primarily target the identification of the 50 most common English first names within the specified text.

## Modules
The application will be composed of the following key modules:

1. **Main Module**:
    - Responsible for reading a large text file in parts (approximately 1000 lines each).
    - Sends each part as a string to a matcher module.
    - Calls the aggregator module to compile and display results after all matchers have completed their tasks.

2. **Matcher Module**:
    - Receives text strings as input.
    - Searches for matches from a predefined set of strings.
    - Outputs a map associating each word with its respective locations in the text.

3. **Aggregator Module**:
    - Aggregates results from all matcher modules.
    - Prints the aggregated results.

## Concurrency
- The system will employ several concurrent matchers, with each matcher operating in a separate thread to optimize performance.
- The results will be printed in no specific order after all sections of the text have been processed.

## Source Text
- The program will use the text available at [http://norvig.com/big.txt](http://norvig.com/big.txt) for its search operations.
- The strings to be searched will consist of the 50 most common English first names.

## Output Example
An example output line from the program, based on the input

## Running with Docker

You can run this Spring Boot application in a Docker container. Follow these steps:

1. Build the Docker image:

   ```bash
   docker build -t TextSearcherV2 -f src/Dockerfile . --no-cache
2. ```bash 
   docker run -p 9093:8080 TextSearcherV2
3. ```bash
   docker stop $(docker ps -q --filter ancestor=TextSearcherV2)

- **Note: Make sure you have Docker installed on your machine to run the application using Docker.**

Feel free to customize the instructions to fit your project's specific needs.
# TextSearcher API

The TextSearcher API provides a set of endpoints for creating, retrieving, and deleting names, especially focusing on auto-completion functionalities. This document outlines the available endpoints and their usage.

## API Endpoints


## Contributing

Contributions to the TextSearcherV2 Service are welcome. Please follow these steps to contribute:

1. Fork the repository and create your branch from `master`.
2. Make your changes and test them thoroughly.
3. Submit a pull request with a comprehensive description of changes.
