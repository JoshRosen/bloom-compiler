var Rx = require('rx');

/**
 * Represents a relation with a (composite) primary key.
 * *
 * @param lastKeyColIndex  the index of the last key column.
 *      Assumes that records are of the form [keyCol1, keyCol2, ... , valCol1, valCol2, ...].
 *      If lastKeyColIndex == len(record) - 1, then the entire record is treated as the key
 *      and the table functions like a set.
 * @constructor
 */
function Table(lastKeyColIndex) {
    'use strict';

    this.records = {};
    var table = this;

    this.insertionStream = new Rx.Subject();

    function getKeyCols(rec) {
        return rec.slice(0, lastKeyColIndex + 1);
    }

    function getValCols(rec) {
        return rec.slice(lastKeyColIndex + 1);
    }

    this.insertRecord = function(rec) {
        var key = getKeyCols(rec);
        var val = getValCols(rec);
        if (key in table.records && table.records[key] !== val) {
            throw new Error("Key constraint violated when inserting " + rec);
        } else {
            table.records[key] = val;
            table.insertionStream.onNext(rec);
        }
    };

    this.deleteRecord = function(rec) {
        var key = getKeyCols(rec);
        var val = getValCols(rec);
        if (table.records[key] === val) {
            var deleted = table.records[key];
            delete table.records[key];
            return deleted;
        } else {
            return undefined;
        }
    };

    this.insert = new Rx.Observer.create(this.insertRecord);


}

module.exports = Table;