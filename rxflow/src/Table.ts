import DataflowElement = require('./DataflowElement');
import InputPort = require('./InputPort');
import OutputPort = require('./OutputPort');
import punctuations = require('./punctuations');


/**
 * Represents a relation with a (composite) primary key.
 */
class Table<T> extends DataflowElement {

    private lastKeyColIndex: number;
    records = {};

    /**
     * @param lastKeyColIndex  the index of the last key column.
     *      Assumes that records are of the form [keyCol1, keyCol2, ... , valCol1, valCol2, ...].
     *      If lastKeyColIndex == len(record) - 1, then the entire record is treated as the key
     *      and the table functions like a set.
     */
    constructor(lastKeyColIndex: number) {
        super();
        this.lastKeyColIndex = lastKeyColIndex;
    }

    insertionStream = new OutputPort<T>(this);

    private getKeyCols(rec) {
        return rec.slice(0, this.lastKeyColIndex + 1);
    }

    private getValCols(rec) {
        return rec.slice(this.lastKeyColIndex + 1);
    }

    private insertRecord(rec) {
        var key = this.getKeyCols(rec);
        var val = this.getValCols(rec);
        if (key in this.records && this.records[key] !== val) {
            throw new Error('Key constraint violated when inserting ' + rec);
        } else {
            this.records[key] = val;
            this.insertionStream.onNext(rec);
        }
    }

    private deleteRecord(rec) {
        var key = this.getKeyCols(rec);
        var val = this.getValCols(rec);
        if (this.records[key] === val) {
            var deleted = this.records[key];
            delete this.records[key];
            return deleted;
        } else {
            return undefined;
        }
    }

    insert = new InputPort(x => this.insertRecord(x), this);

    endRound(round: number) {
        this.insert.onNext(new punctuations.EndOfRound(round));
    }

}

export = Table;
