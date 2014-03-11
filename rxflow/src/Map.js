var __extends = this.__extends || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    __.prototype = b.prototype;
    d.prototype = new __();
};
var DataflowElement = require('./DataflowElement');
var InputPort = require('./InputPort');
var OutputPort = require('./OutputPort');

var Map = (function (_super) {
    __extends(Map, _super);
    function Map(mapFunc) {
        var _this = this;
        _super.call(this);
        this.output = new OutputPort(this);
        this.input = new InputPort(function (x) {
            return _this.output.onNext(_this.mapFunc(x));
        }, this);
        this.mapFunc = mapFunc;
    }
    return Map;
})(DataflowElement);

module.exports = Map;
//# sourceMappingURL=Map.js.map
