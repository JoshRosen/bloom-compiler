var InputPort = (function () {
    function InputPort(onNext) {
        this.producers = [];
        this.onNext = onNext;
    }
    InputPort.prototype.onCompleted = function () {
        // Intentionally left blank
    };
    return InputPort;
})();

module.exports = InputPort;
//# sourceMappingURL=InputPort.js.map
