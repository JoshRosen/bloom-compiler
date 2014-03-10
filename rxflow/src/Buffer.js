var __extends = this.__extends || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    __.prototype = b.prototype;
    d.prototype = new __();
};
var DataflowElement = require('./DataflowElement');
var InputPort = require('./InputPort');
var OutputPort = require('./OutputPort');

var Buffer = (function (_super) {
    __extends(Buffer, _super);
    function Buffer() {
        _super.apply(this, arguments);
        var _this = this;
        this.buffer = [];
        this.input = new InputPort(function (x) {
            return _this.buffer.push(x);
        });
        this.output = new OutputPort();
    }
    Buffer.prototype.invalidate = function () {
        this.buffer = [];
    };

    Buffer.prototype.isEmpty = function () {
        return this.buffer.length === 0;
    };

    Buffer.prototype.flush = function () {
        var _this = this;
        var oldBuffer = this.buffer;
        this.buffer = [];
        oldBuffer.forEach(function (x) {
            return _this.output.onNext(x);
        });
        return oldBuffer.length;
    };
    return Buffer;
})(DataflowElement);

module.exports = Buffer;
//# sourceMappingURL=Buffer.js.map
