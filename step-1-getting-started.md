# Step 1: Getting Started

Before getting started, let's make sure you have everything you need for running this demo.

## Prerequisites

### Install Java 17 or newer

You'll need Java 17 or newer for this workshop.
Testcontainers libraries are compatible with Java 8+, but this workshop uses a Spring Boot 3.x application which requires Java 17 or newer.

We would recommend using [SDKMAN](https://sdkman.io/) to install Java on your machine if you are using MacOS, Linux or Windows WSL.

### Install Docker

You need to have a Docker environment to use Testcontainers.

```shell
$ docker version

Client:
 Cloud integration: v1.0.35
 Version:           24.0.2
 API version:       1.43
 Go version:        go1.20.4
 Git commit:        cb74dfc
 Built:             Thu May 25 21:51:16 2023
 OS/Arch:           darwin/arm64
 Context:           desktop-linux

Server: Docker Desktop 4.21.1 (114176)
 Engine:
  Version:          24.0.2
  API version:      1.43 (minimum version 1.12)
  Go version:       go1.20.4
  Git commit:       659604f
  Built:            Thu May 25 21:50:59 2023
  OS/Arch:          linux/arm64
  Experimental:     false
 ...
```

## Download the project

Clone the [microcks-testcontainers-java-spring-demo](https://github.com/microcks/microcks-testcontainers-java-spring-demo) repository from GitHub to your computer:

```shell
git clone https://github.com/microcks/microcks-testcontainers-java-spring-demo.git
```

## Compile the project to download the dependencies

With Maven:

```shell
./mvnw compile
```

### 

[Next](step-2-exploring-the-app.md)