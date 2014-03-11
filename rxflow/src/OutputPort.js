var OutputPort = (function () {
    function OutputPort(elem) {
        this.consumers = [];
        if (elem != null) {
            elem.registerOutput(this);
        }
    }
    OutputPort.prototype.onNext = function (val) {
        this.consumers.forEach(function (consumer) {
            return consumer.onNext(val);
        });
    };

    OutputPort.prototype.subscribe = function (inputPort) {
        this.consumers.push(inputPort);
        inputPort.addProducer(this);
    };
    return OutputPort;
})();

module.exports = OutputPort;
//# sourceMappingURL=OutputPort.js.map
