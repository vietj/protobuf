## Conformance Docker file

Build a Docker container for testing Protobuf conformance.

The image builds the Protobuf conformance test runner using Bazel.

The image runs the conformance test runner, it expects the following files to be available

- `/conformance-runner.jar` - the fat executing conformance tests
- `/known_failures.txt` - the conformance runner expected failures

```
> docker build -t protobuf/conformance protobuf-conformance/src/test/docker/conformance
```

Running the Protobuf conformance container

```
> docker run --rm -it --name conformance --mount type=bind,source=/Users/julien/java/protobuf-project/protobuf-conformance/target/conformance-runner.jar,target=/conformance-runner.jar --mount type=bind,source=/Users/julien/java/protobuf-project/protobuf-conformance/known_failures.txt,target=/known_failures.txt protobuf/conformance
```
