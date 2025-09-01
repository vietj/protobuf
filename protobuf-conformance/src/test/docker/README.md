## Conformance Docker file

Build container

```
> docker build -t protobuf/conformance postgres
```

Running the container

```
> docker run --rm --name test-postgres_tc -p 5432:5432 --cap-add=NET_ADMIN test/postgres_tc
```
