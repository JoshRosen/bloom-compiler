var __extends = this.__extends || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    __.prototype = b.prototype;
    d.prototype = new __();
};
var punctuations = require('./punctuations');

var InputPort = (function (_super) {
    __extends(InputPort, _super);
    function InputPort(onNextValue, elem) {
        if (typeof elem === "undefined") { elem = null; }
        _super.call(this);
        this.producers = [];
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
        if (val instanceof punctuations.Punctuation) {
            this.handlePunctuation(val, null);
        } else {
            this.onNextValue(val);
        }
    };

    InputPort.prototype.getNumInputs = function () {
        return this.producers.length;
    };

    InputPort.prototype.sendPunctuationDownstream = function (punc) {
        if (this.elem != null) {
            this.elem.handlePunctuation(punc, this);
        }
    };
    return InputPort;
})(punctuations.PunctuationHandlerMixin);

module.exports = InputPort;
//# sourceMappingURL=InputPort.js.map
