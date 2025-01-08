# Step 1: Getting Started

Before getting started, let's make sure you have everything you need for running this demo.

## Prerequisites

### Install Java 17 or newer

You'll need Java 17 or newer for this workshop.
Testcontainers libraries are compatible with Java 8+, but this workshop uses a Spring Boot 3.x application which requires Java 17 or newer.

We would recommend using [SDKMAN](https://sdkman.io/) to install Java on your machine if you are using MacOS, Linux or Windows WSL.

### Install Docker

You need to have a [Docker](https://docs.docker.com/get-docker/) or [Podman](https://podman.io/) environment to use Testcontainers.

```shell
$ docker version

Client:
 Version:           27.3.1
 API version:       1.47
 Go version:        go1.22.7
 Git commit:        ce12230
 Built:             Fri Sep 20 11:38:18 2024
 OS/Arch:           darwin/arm64
 Context:           desktop-linux

Server: Docker Desktop 4.36.0 (175267)
 Engine:
  Version:          27.3.1
  API version:      1.47 (minimum version 1.24)
  Go version:       go1.22.7
  Git commit:       41ca978
  Built:            Fri Sep 20 11:41:19 2024
  OS/Arch:          linux/arm64
  Experimental:     false
 containerd:
  Version:          1.7.21
  GitCommit:        472731909fa34bd7bc9c087e4c27943f9835f111
 runc:
  Version:          1.1.13
  GitCommit:        v1.1.13-0-g58aa920
 docker-init:
  Version:          0.19.0
  GitCommit:        de40ad0
```

## Download the project

Clone the [microcks-testcontainers-java-spring-demo](https://github.com/microcks/microcks-testcontainers-java-spring-demo) repository from GitHub to your computer:

```shell
git clone https://github.com/microcks/microcks-testcontainers-java-spring-demo.git
```

## Compile the project to download the dependencies

With Maven:

```shell
./mvnw clean package -DskipTests
```

### 

[Next](step-2-exploring-the-app.md)