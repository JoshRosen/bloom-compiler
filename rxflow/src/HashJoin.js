var __extends = this.__extends || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    __.prototype = b.prototype;
    d.prototype = new __();
};
var DataflowElement = require('./DataflowElement');
var InputPort = require('./InputPort');
var OutputPort = require('./OutputPort');

var HashJoin = (function (_super) {
    __extends(HashJoin, _super);
    function HashJoin(leftKeyFunc, rightKeyFunc, buildInput) {
        var _this = this;
        _super.call(this);
        this.output = new OutputPort(this);
        this.hashTable = {};
        if (buildInput === 'left') {
            this.leftInput = new InputPort(function (x) {
                return _this.handleBuildInput(x);
            });
            this.rightInput = new InputPort(function (x) {
                return _this.handleProbeInput(x);
            });
            this.buildInput = this.leftInput;
            this.probeInput = this.rightInput;
            this.buildKeyFunc = leftKeyFunc;
            this.probeKeyFunc = rightKeyFunc;
            this.resultOrderingFunction = function (b, p) {
                return [b, p];
            };
        } else if (buildInput === 'right') {
            this.rightInput = new InputPort(function (x) {
                return _this.handleBuildInput(x);
            });
            this.leftInput = new InputPort(function (x) {
                return _this.handleProbeInput(x);
            });
            this.buildInput = this.rightInput;
            this.probeInput = this.leftInput;
            this.buildKeyFunc = rightKeyFunc;
            this.probeKeyFunc = leftKeyFunc;
            this.resultOrderingFunction = function (b, p) {
                return [p, b];
            };
        } else {
            throw new Error('buildInput should be \'left\' or \'right\', not \'' + buildInput + '\'');
        }
    }
    HashJoin.prototype.handleProbeInput = function (p) {
        var _this = this;
        var key = this.probeKeyFunc(p);
        if (key in this.hashTable) {
            var matches = this.hashTable[key];
            matches.forEach(function (b) {
                return _this.output.onNext(_this.resultOrderingFunction(b, p));
            });
        }
    };

    HashJoin.prototype.handleBuildInput = function (b) {
        var key = this.buildKeyFunc(b);
        if (!(key in this.hashTable)) {
            this.hashTable[key] = [b];
        } else {
            this.hashTable[key].push(b);
        }
    };
    return HashJoin;
})(DataflowElement);

module.exports = HashJoin;
//# sourceMappingURL=HashJoin.js.map
