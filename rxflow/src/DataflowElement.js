var punctuations = require('./punctuations');

/**
* Base class for RxFlow dataflow elements.
*/
var DataflowElement = (function () {
    function DataflowElement() {
        this.inputs = [];
        this.outputs = [];
        this.eorCount = 0;
    }
    DataflowElement.prototype.registerInput = function (input) {
        this.inputs.push(input);
    };

    DataflowElement.prototype.registerOutput = function (output) {
        this.outputs.push(output);
    };

    DataflowElement.prototype.handlePunctuation = function (punc, port) {
        if (punc === punctuations.END_OF_ROUND) {
            this.eorCount += 1;
            if (this.eorCount === this.inputs.length) {
                this.flush();
                this.outputs.forEach(function (output) {
                    return output.onNext(punctuations.END_OF_ROUND);
                });
                this.eorCount = 0;
            }
        }
    };

    DataflowElement.prototype.flush = function () {
        // This space intentionally left empty
    };

    DataflowElement.prototype.invalidate = function () {
        // This space intentionally left empty
    };
    return DataflowElement;
})();

module.exports = DataflowElement;
//# sourceMappingURL=DataflowElement.js.map
