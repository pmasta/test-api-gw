Overview
description_reader is a simple Java application that:

Analyzes source code,

Generates descriptions or summaries,

Automatically sends them to the Chroma vector database.

Prerequisites
Java 17 or newer (JDK)

The Chroma backend must be running and accessible (default: http://localhost:5000)

1. Compile the application
Navigate to the description_reader directory and compile the Java files:

bash
Kopiuj
Edytuj
cd description_reader
javac DescriptionReader.java
(Replace DescriptionReader.java with the actual entry point if it's different.)

2. Run the application
bash
Kopiuj
Edytuj
java DescriptionReader \
  --source-path ../your-code-directory \
  --chroma-endpoint http://localhost:5000
Replace your-code-directory with the path to the project you want to analyze.

Optional Parameters
If your script supports arguments, you can pass them via command line, such as:

--language java

--recursive true

--log true
