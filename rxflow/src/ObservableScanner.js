/// <reference path="../typings/rx.js/rx.d.ts" />
var __extends = this.__extends || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    __.prototype = b.prototype;
    d.prototype = new __();
};
var Rx = require('rx');
var Buffer = require('./Buffer');

var DataflowElement = require('./DataflowElement');

var ObservableScanner = (function (_super) {
    __extends(ObservableScanner, _super);
    function ObservableScanner(observable) {
        var _this = this;
        _super.call(this);
        this.buffer = new Buffer();
        this.output = this.buffer.output;
        observable.subscribe(Rx.Observer.create(function (x) {
            return _this.buffer.input.onNext(x);
        }));
    }
    ObservableScanner.prototype.flush = function () {
        return this.buffer.flush();
    };
    return ObservableScanner;
})(DataflowElement);

module.exports = ObservableScanner;
//# sourceMappingURL=ObservableScanner.js.map
