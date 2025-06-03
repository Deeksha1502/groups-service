# PR Code Coverage

This document outlines the steps for the Pull Request Code Coverage process.

## Triggering the Workflow
This GitHub Actions workflow is triggered automatically on every pull request to any branch in the repository. It ensures comprehensive testing and quality checks for all code changes.

## Prerequisites
- Maven
- JDK 11 (for building and testing)
- JDK 17 (for SonarQube analysis)
- SonarCloud account and token

## Steps

1. **Checkout the Repository**
   - The first step is to checkout the repository to ensure you have the latest code.

2. **Set Up JDK 11**
   - JDK 11 is used for building the project. The workflow uses the `temurin` distribution and caches Maven dependencies for faster builds.

3. **Build the Project and Generate Reports**
   - Execute the following commands to build the project and generate test coverage reports:
     ```bash
     mvn clean install -DskipTests
     cd service
     mvn clean verify jacoco:report surefire-report:report
     ```

4. **Generate Test Summary**
   - The workflow generates an interactive test summary showing all test results, including passed and failed tests, grouped by test class.

5. **Set Up JDK 17**
   - After generating the coverage report, set up JDK 17 for SonarQube analysis.

6. **Run SonarQube Analysis**
   - Execute the SonarQube analysis to check the quality of the code and coverage. The analysis includes:
     - Code quality metrics
     - Code coverage analysis
     - Security vulnerabilities
   ```bash
   mvn sonar:sonar \
     -Dsonar.projectKey=sunbird-lern \
     -Dsonar.organization=sunbird-lern \
     -Dsonar.host.url=https://sonarcloud.io \
     -Dsonar.coverage.jacoco.xmlReportPaths=service/target/site/jacoco/jacoco.xml
    ```
## Generated Reports
- **Test Reports**: `service/target/surefire-reports/`
- **Code Coverage**: `service/target/site/jacoco/`

## Notes
- Ensure the `SONAR_TOKEN` secret is configured in your GitHub repository settings for SonarQube analysis to work
- The test summary is available directly in the GitHub Actions UI
- The workflow automatically runs on every pull request to any branch
