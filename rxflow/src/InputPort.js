var punctuations = require('./punctuations');

var InputPort = (function () {
    function InputPort(onNextValue, elem) {
        if (typeof elem === "undefined") { elem = null; }
        this.producers = [];
        this.eorCount = 0;
        this.onNextValue = onNextValue;
        this.elem = elem;
        if (elem != null) {
            elem.registerInput(this);
        }
    }
    InputPort.prototype.addProducer = function (producer) {
        this.producers.push(producer);
    };

    InputPort.prototype.onNext = function (val) {
        if (val === punctuations.END_OF_ROUND) {
            this.eorCount += 1;
            if (this.eorCount === this.producers.length) {
                if (this.elem != null) {
                    this.elem.handlePunctuation(punctuations.END_OF_ROUND, this);
                }
                this.eorCount = 0;
            }
        } else {
            this.onNextValue(val);
        }
    };
    return InputPort;
})();

module.exports = InputPort;
//# sourceMappingURL=InputPort.js.map
