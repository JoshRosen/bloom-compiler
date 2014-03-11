var __extends = this.__extends || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    __.prototype = b.prototype;
    d.prototype = new __();
};
var DataflowElement = require('./DataflowElement');
var InputPort = require('./InputPort');
var OutputPort = require('./OutputPort');

var TableScanner = (function (_super) {
    __extends(TableScanner, _super);
    function TableScanner(table) {
        var _this = this;
        _super.call(this);
        this.output = new OutputPort(this);
        this.input = new InputPort(function (x) {
            return _this.output.onNext(x);
        }, this);
        this.table = table;
        table.insertionStream.subscribe(this.input);
    }
    TableScanner.prototype.rescan = function () {
        for (var key in this.table.records) {
            if (true) {
                this.output.onNext(key + this.table.records[key]);
            }
        }
    };
    return TableScanner;
})(DataflowElement);

module.exports = TableScanner;
//# sourceMappingURL=TableScanner.js.map
