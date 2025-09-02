FROM ubuntu:20.04
RUN apt-get -y update
RUN apt-get -y install curl g++ unzip zip socat openjdk-17-jdk
RUN curl -LO https://github.com/bazelbuild/bazelisk/releases/download/v1.15.0/bazelisk-linux-amd64 && \
    chmod 755 bazelisk-linux-amd64 && \
    mv bazelisk-linux-amd64 /usr/bin/bazelisk
RUN curl -LO https://github.com/protocolbuffers/protobuf/releases/download/v32.0/protobuf-32.0.tar.gz
RUN tar -xvzf protobuf-32.0.tar.gz
RUN cd ./protobuf-32.0 && USE_BAZEL_VERSION=7.1.2 BAZEL_CXXOPTS="-std=c++17" /usr/bin/bazelisk build conformance:conformance_test_runner
RUN mv ./protobuf-32.0/bazel-bin/conformance/conformance_test_runner /usr/bin/conformance_test_runner
COPY conformance.sh /conformance.sh
RUN chmod +x /conformance.sh
CMD /usr/bin/conformance_test_runner --enforce_recommended --maximum_edition PROTO3 --output_dir . --failure_list known_failures.txt /conformance.sh
