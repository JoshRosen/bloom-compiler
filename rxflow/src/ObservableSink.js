/// <reference path="../typings/rx.js/rx.d.ts" />
var __extends = this.__extends || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    __.prototype = b.prototype;
    d.prototype = new __();
};
var Rx = require('rx');
var DataflowElement = require('./DataflowElement');
var InputPort = require('./InputPort');

var ObservableSink = (function (_super) {
    __extends(ObservableSink, _super);
    function ObservableSink() {
        _super.apply(this, arguments);
        var _this = this;
        this.output = new Rx.Subject();
        this.input = new InputPort(function (x) {
            return _this.output.onNext(x);
        }, this);
    }
    return ObservableSink;
})(DataflowElement);

module.exports = ObservableSink;
//# sourceMappingURL=ObservableSink.js.map
