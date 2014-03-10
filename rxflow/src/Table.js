var InputPort = require('./InputPort');
var OutputPort = require('./OutputPort');

/**
* Represents a relation with a (composite) primary key.
*/
var Table = (function () {
    /**
    * @param lastKeyColIndex  the index of the last key column.
    *      Assumes that records are of the form [keyCol1, keyCol2, ... , valCol1, valCol2, ...].
    *      If lastKeyColIndex == len(record) - 1, then the entire record is treated as the key
    *      and the table functions like a set.
    */
    function Table(lastKeyColIndex) {
        var _this = this;
        this.records = {};
        this.insertionStream = new OutputPort();
        this.insert = new InputPort(function (x) {
            return _this.insertRecord(x);
        });
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
})();

module.exports = Table;
//# sourceMappingURL=Table.js.map
