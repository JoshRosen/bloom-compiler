var OutputPort = (function () {
    function OutputPort() {
        this.consumers = [];
    }
    OutputPort.prototype.onNext = function (val) {
        this.consumers.forEach(function (consumer) {
            return consumer.onNext(val);
        });
    };

    OutputPort.prototype.subscribe = function (inputPort) {
        this.consumers.push(inputPort);
        inputPort.producers.push(this);
    };
    return OutputPort;
})();

module.exports = OutputPort;
//# sourceMappingURL=OutputPort.js.map
