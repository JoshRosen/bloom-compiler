var __extends = this.__extends || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    __.prototype = b.prototype;
    d.prototype = new __();
};
var DataflowElement = require('./DataflowElement');
var InputPort = require('./InputPort');
var OutputPort = require('./OutputPort');

/**
* Represents a relation with a (composite) primary key.
*/
var Table = (function (_super) {
    __extends(Table, _super);
    /**
    * @param lastKeyColIndex  the index of the last key column.
    *      Assumes that records are of the form [keyCol1, keyCol2, ... , valCol1, valCol2, ...].
    *      If lastKeyColIndex == len(record) - 1, then the entire record is treated as the key
    *      and the table functions like a set.
    */
    function Table(lastKeyColIndex) {
        var _this = this;
        _super.call(this);
        this.records = {};
        this.insertionStream = new OutputPort(this);
        this.insert = new InputPort(function (x) {
            return _this.insertRecord(x);
        }, this);
        this.lastKeyColIndex = lastKeyColIndex;
    }
    Table.prototype.getKeyCols = function (rec) {
        return rec.slice(0, this.lastKeyColIndex + 1);
    };

    Table.prototype.getValCols = function (rec) {
        return rec.slice(this.lastKeyColIndex + 1);
    };

    Table.prototype.insertRecord = function (rec) {
        var key = this.getKeyCols(rec);
        var val = this.getValCols(rec);
        if (key in this.records && this.records[key] !== val) {
            throw new Error('Key constraint violated when inserting ' + rec);
        } else {
            this.records[key] = val;
            this.insertionStream.onNext(rec);
        }
    };

    Table.prototype.deleteRecord = function (rec) {
        var key = this.getKeyCols(rec);
        var val = this.getValCols(rec);
        if (this.records[key] === val) {
            var deleted = this.records[key];
            delete this.records[key];
            return deleted;
        } else {
            return undefined;
        }
    };
    return Table;
})(DataflowElement);

module.exports = Table;
//# sourceMappingURL=Table.js.map
