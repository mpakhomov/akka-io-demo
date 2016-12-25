# akka-io-demo

How to run:
1. python scripts/upstream.py
2. start up com.mpakhomov.ServerApp
3. start up any number of clients: com.mpakhomov.ClientApp

DataFlow:

Upstream:
upstream tcp server -> UpstreamClientActor -> EventProcessorActor -> CandlestickAggregatorActor

Server:
CandlestickAggregatorActor -> ServerActor