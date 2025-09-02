#!/bin/bash
exec java -jar conformance-runner.jar "$@"
#socat STDIO TCP4-LISTEN:4000
#socat STDIO TCP4:host.testcontainers.internal:4000
