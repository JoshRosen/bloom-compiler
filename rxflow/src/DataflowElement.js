var __extends = this.__extends || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    __.prototype = b.prototype;
    d.prototype = new __();
};
var punctuations = require('./punctuations');

/**
* Base class for RxFlow dataflow elements.
*/
var DataflowElement = (function (_super) {
    __extends(DataflowElement, _super);
    function DataflowElement() {
        _super.apply(this, arguments);
        this.inputs = [];
        this.outputs = [];
    }
    DataflowElement.prototype.registerInput = function (input) {
        this.inputs.push(input);
    };

    DataflowElement.prototype.registerOutput = function (output) {
        this.outputs.push(output);
    };

    DataflowElement.prototype.handleEndOfRound = function () {
        this.flush();
    };

    DataflowElement.prototype.getNumInputs = function () {
        return this.inputs.length;
    };

    DataflowElement.prototype.sendPunctuationDownstream = function (punc) {
        this.outputs.forEach(function (output) {
            return output.onNext(punc);
        });
    };

    DataflowElement.prototype.flush = function () {
        // This space intentionally left empty
    };

    DataflowElement.prototype.invalidate = function () {
        // This space intentionally left empty
    };
    return DataflowElement;
})(punctuations.PunctuationHandlerMixin);

module.exports = DataflowElement;
//# sourceMappingURL=DataflowElement.js.map
