# GitHub Action: Build and Deploy

## Overview
This GitHub Action automates the build and deployment process for the Groups service. It builds the project, runs tests, packages the application, and pushes a Docker image to GitHub Container Registry (GHCR).

## Usage
The action will automatically build and deploy your service whenever a new tag is pushed.

## Steps

1. **Set up JDK 11**
   - Configures the environment with JDK 11 using the `actions/setup-java` action with Temurin distribution.

2. **Checkout code**
   - Checks out the repository code with full commit history.

3. **Cache Maven packages**
   - Caches Maven dependencies to speed up subsequent builds.

4. **Build and run test cases**
   ```bash
   mvn clean install -DskipTests
   cd service
   mvn clean verify surefire-report:report
   ```
   - Generates test reports for the test results.

5. **Package build artifact**
   - Packages the application using Play Framework's `dist` goal.
   ```bash
   mvn -f service/pom.xml play2:dist
   ```

6. **Upload artifact**
   - Uploads the packaged application as a GitHub Actions artifact named `groups-service-dist`.

7. **Extract image tag details**
   - Prepares Docker image name and tags based on the repository name and reference.

8. **Log in to GitHub Container Registry**
   - Authenticates to GHCR using the provided GitHub token.

9. **Build and push Docker image**
   - Builds the Docker image using the provided Dockerfile and pushes it to GHCR with the appropriate tags.

## Environment Variables
- `REGISTRY`: The GitHub Container Registry URL (ghcr.io)
- `IMAGE_NAME`: Auto-generated based on the repository name (e.g., ghcr.io/username/repo)
- `IMAGE_TAG`: Auto-generated based on the git reference (branch name or tag)

## Permissions
This workflow requires the following permissions:
- `contents: read` - To read the repository contents
- `packages: write` - To push Docker images to GitHub Container Registry

## How to Use the Docker Image

1. **Pull the Docker Image**:
   ```bash
   docker pull ghcr.io/<repository-name>:<tag>
   ```
   Replace `<repository-name>` and `<tag>` with the appropriate values.

2. **Run the Docker Container**:
   ```bash
   docker run -d -p <host-port>:9000 ghcr.io/<repository-name>:<tag>
   ```
   Replace `<host-port>` with your desired port number.

3. **Access the Application**:
   Once the container is running, you can access the application at `http://localhost:<host-port>`.

## Notes
- The workflow uses Ubuntu latest runner.
- Maven dependencies are cached to improve build performance.
- The Docker image is built using the Dockerfile in the repository root.
- The workflow automatically handles repository name and tag generation for Docker images.
- Test reports are generated and made available in the GitHub Actions UI.
