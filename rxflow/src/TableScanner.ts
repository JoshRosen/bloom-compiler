import Buffer = require('./Buffer');
import DataflowElement = require('./DataflowElement');
import InputPort = require('./InputPort');
import OutputPort = require('./OutputPort');
import Table = require('./Table');



class TableScanner<T> extends DataflowElement {

    output = new OutputPort<T>();

    private table: Table<T>;
    private buffer = new Buffer();
    private input = new InputPort<T>(x => this.output.onNext(x));

    constructor(table: Table<T>) {
        super();
        this.table = table;
        table.insertionStream.subscribe(this.input);
    }

    rescan() {
        for (var key in this.table.records) {
            if (true) { // Suppress tslint warning
                this.output.onNext(key + this.table.records[key]);
            }
        }
    }

    invalidate() {
        this.buffer.invalidate();
    }

    flush() {
        return this.buffer.flush();
    }
}

export = TableScanner;
