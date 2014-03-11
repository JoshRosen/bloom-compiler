var __extends = this.__extends || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    __.prototype = b.prototype;
    d.prototype = new __();
};
var DataflowElement = require('./DataflowElement');
var InputPort = require('./InputPort');
var OutputPort = require('./OutputPort');
var Aggregate = require('./Aggregate');

var ArgMin = (function (_super) {
    __extends(ArgMin, _super);
    function ArgMin(keyFunction, orderingFields, orderingFunction) {
        var _this = this;
        _super.call(this);
        this.output = new OutputPort(this);
        var aggregateFunction = function () {
            var value = null;
            this.getValue = function () {
                return value;
            };
            this.next = function (x) {
                if (value === null || orderingFunction(orderingFields(x), orderingFields(value))) {
                    value = x;
                }
            };
        };
        this.aggregate = new Aggregate(keyFunction, [aggregateFunction]);
        this.input = this.aggregate.input;
        var outputProjector = new InputPort(function (x) {
            return _this.output.onNext(x[1]);
        }, null);
        this.aggregate.output.subscribe(outputProjector);
    }
    ArgMin.prototype.flush = function () {
        this.aggregate.flush();
    };

    ArgMin.prototype.invalidate = function () {
        this.aggregate.invalidate();
    };
    return ArgMin;
})(DataflowElement);

module.exports = ArgMin;
//# sourceMappingURL=ArgMin.js.map
