# Websocket client for blockchain data, with integration into GemFire and GPDB

## Ideas for applications

* Graph analysis of blockchain transactions
* Plot transaction volumes over time, overlaid onto other data (news, commodities, gold, stock market indexes)
* Plot blockchain / USD value over time, possibly correlated as above
* Look for features indicative of the rebalancing adjustment made approximately every 14 days

## Components with function

1. Spring Boot app
    * Pulls in blockchain transaction data via a websocket
    * Stores that into GemFire
    * Serves the stream up via its own websocket server

1. GemFire
    * Stores data in memory for real time dashboard (histogram, etc.)
    * WAN replicated across > 1 IaaS (AWS, Azure, Google Cloud)

1. GPDB
    * Warm data in heap tables
    * Cold data in S3 bucket
    * Large scale analytical queries, statistical, and machine learning analysis

## External dependencies

1. Data source: `wss://ws.blockchain.info/inv`

1. Java websocket library must be downloaded, built, and installed:
    ```
    $ git clone https://github.com/TooTallNate/Java-WebSocket.git
    $ cd ./Java-WebSocket/
    $ mvn clean install
    ```

## Data sample

```
{
  "op" : "utx",
  "x" : {
    "lock_time" : 492679,
    "ver" : 2,
    "size" : 226,
    "inputs" : [ {
      "sequence" : 4294967294,
      "prev_out" : {
        "spent" : true,
        "tx_index" : 297367747,
        "type" : 0,
        "addr" : "1Mo9qpihFTvJBE1vEh1p1bxuGKgnkg9at",
        "value" : 739787106,
        "n" : 0,
        "script" : "76a91403eed86942c88cd2669d3e94e34babe024ffa8dd88ac"
      },
      "script" : "483045022100ce192f3d8cdb1fb11a056369df83b86924b982bebcfceb5786cb2a8e07e7d2c402206ea1b5d80c3b457967a6dc84e54992875d4d2a7da5c71bc3137f2ce9521fc7e6012102c5ce11d10ac960fd0004a9d3c1ca736fab9821ecdaecd31262e0e42120d91cf9"
    } ],
    "time" : 1509613962,
    "tx_index" : 297369076,
    "vin_sz" : 1,
    "hash" : "138df026f4db0d38aae2d06091eb235170f6199a01c7303507dc72d131ca684f",
    "vout_sz" : 2,
    "relayed_by" : "0.0.0.0",
    "out" : [ {
      "spent" : false,
      "tx_index" : 297369076,
      "type" : 0,
      "addr" : "1KgcEX7mYJVy4qJqjjq2a8qCXUEgxSxY36",
      "value" : 738483656,
      "n" : 0,
      "script" : "76a914ccefe5d1055c51cbbba2d3515b92957e7081c9da88ac"
    }, {
      "spent" : false,
      "tx_index" : 297369076,
      "type" : 0,
      "addr" : "1Ek6E3PdfsfkuyCdjgCoKstesqiQ3t2hM6",
      "value" : 1258250,
      "n" : 1,
      "script" : "76a91496bfe20e3f51cd0c7f7543f2c0ceafdb4b97b81888ac"
    } ]
  }
}
```

## Install and start GemFire

* Save this directory: `pushd .`
* Download the archive from Pivotal Network
* Extract somewhere: `cd /opt/ ; sudo unzip ~/Downloads/pivotal-gemfire-9.1.1`
* Set `PATH`: `cd ./pivotal-gemfire-9.1.1/ ; export PATH=$PWD/bin:$PATH`
* Change back into this GitHub repo directory: `popd`
* Start a locator: `gfsh -e "start locator --name=locator --classpath=$PWD/target/classes"`
* Start a server: `gfsh -e "start server --name=server --cache-xml-file=./src/main/resources/serverCache.xml --locators=localhost[10334] --classpath=$PWD/target/classes"`
* Start up the Pulse web UI: `gfsh -e "start pulse"`
* This should direct your browser to the Pulse UI, where you enter _admin_ for user name, and _admin_ for password

## Potentially Useful References
* An OReilly [title](http://chimera.labs.oreilly.com/books/1234000001802/ch05.html#tx_lifecycle)
* To redirect from a Spring Boot endpoint to our WS server, you can just return `redirect:<uri>` from a `@Controller`, or a `RedirectView`
* A [note](https://stackoverflow.com/questions/39202243/how-to-create-composite-primary-key-scenario-in-gemfire-region) on composite primary keys in Gemfire
* Video: [Demystifying High Availability with Apache Geode](https://youtu.be/yachT1xoQww)
* Video: [How To Download and Run Apache Geode](https://youtu.be/zpko_fROWrU)
* [GitHub repo](https://github.com/kdunn926/blockchainviz) for the Javascript UI

