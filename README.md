
# digital-platform-reporting-stubs

This is a simple stub service to simulate calls to EIS (IF).

## Running

This application runs on port `20000`, by default.

``` 
sbt run
```

## Tests

Several integration tests have been included, which cover much of the above demonstration.

```
sbt "it/test"
```
## Dev

### Verification

Before pushing, you can run this [verification script](verify.sh):

```
./verify.sh
```

Or, to format and verify you can run the following:

``` 
./format.sh && ./verify.sh
```


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").