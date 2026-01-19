## Conformance Docker Image

This directory contains files for building and running the Protobuf conformance test suite against the Vert.x Protobuf implementation.

### Prerequisites

- Docker must be installed and running
- The project must be built to generate the conformance runner JAR:
  ```
  mvn package -pl protobuf-conformance -am
  ```

### Docker Image Contents

The Docker image:
- Uses Ubuntu 20.04 as base
- Builds the official Google Protobuf conformance test runner using Bazel
- Expects mounted files for the implementation under test

### Building the Docker Image

```bash
docker build -t protobuf/conformance protobuf-conformance/src/test/docker/conformance
```

### Running the Conformance Tests Manually

From the project root directory:

```bash
docker run --rm -it \
  --mount type=bind,source=$(pwd)/protobuf-conformance/target/conformance-runner.jar,target=/conformance-runner.jar \
  --mount type=bind,source=$(pwd)/protobuf-conformance/known_failures.txt,target=/known_failures.txt \
  protobuf/conformance
```

### Running via Maven Tests

The `ConformanceTest` class in `src/test/java` uses Testcontainers to automatically:
1. Build the Docker image
2. Mount the required files
3. Run the conformance suite
4. Verify the results

Run the test with:
```bash
mvn test -pl protobuf-conformance -Dtest=ConformanceTest
```

Note: The test will be skipped if Docker is not available.

### Files

- `Dockerfile` - Builds the conformance test runner image
- `conformance.sh` - Shell script that invokes the Java conformance implementation

### Known Failures

The `known_failures.txt` file in the module root lists tests that are expected to fail. These are typically:
- Features not yet implemented (e.g., `Any` type handling)
- Edge cases with specific encoding requirements
- JSON validation differences

The conformance suite will pass as long as only the listed tests fail.
