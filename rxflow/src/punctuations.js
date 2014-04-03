var __extends = this.__extends || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    __.prototype = b.prototype;
    d.prototype = new __();
};
var Punctuation = (function () {
    function Punctuation() {
    }
    return Punctuation;
})();
exports.Punctuation = Punctuation;

/**
* Indicates the end of a batch of input.  This is roughly equivalent to "end-of-stream", except
* it doesn't wipe out all operator state.
*/
var EndOfRound = (function (_super) {
    __extends(EndOfRound, _super);
    function EndOfRound(round) {
        _super.call(this);
        this.round = round;
    }
    return EndOfRound;
})(Punctuation);
exports.EndOfRound = EndOfRound;

/**
* Mixin trait that implements the state machine governing punctuation propagation.
*/
var PunctuationHandlerMixin = (function () {
    function PunctuationHandlerMixin() {
        this.currentRound = 0;
        this.eorCount = 0;
    }
    PunctuationHandlerMixin.prototype.getNumInputs = function () {
        throw new Error('getNumInputs() is abstract and must be implemented by subclasses');
    };

    PunctuationHandlerMixin.prototype.handleEndOfRound = function () {
        this.flush();
    };

    PunctuationHandlerMixin.prototype.flush = function () {
        // This space intentionally left blank
    };

    PunctuationHandlerMixin.prototype.sendPunctuationDownstream = function (punc) {
        throw new Error('sendPunctuationDownstream() is abstract and must be implemented by subclasses');
    };

    PunctuationHandlerMixin.prototype.handlePunctuation = function (punc, source) {
        var numInputs = this.getNumInputs();
        if (punc instanceof EndOfRound && punc.round === this.currentRound) {
            this.eorCount += 1;
            if (this.eorCount === numInputs || numInputs === 0) {
                this.eorCount = 0;
                this.currentRound += 1;
                this.handleEndOfRound();
                this.sendPunctuationDownstream(punc);
            }
        }
    };
    return PunctuationHandlerMixin;
})();
exports.PunctuationHandlerMixin = PunctuationHandlerMixin;
//# sourceMappingURL=punctuations.js.map
