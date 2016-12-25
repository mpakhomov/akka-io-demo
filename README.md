# akka-io-demo

DataFlow:

Upstream:
upstream tcp server -> UpstreamClientActor -> EventProcessorActor -> CandlestickAggregatorActor

Server:
CandlestickAggregatorActor -> ServerActor