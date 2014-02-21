var Rx = require('rx');
var Buffer = require('./buffer');


function TableScanner(table) {
    'use strict';

    var scanner = this;

    this.output = new Rx.Subject();

    var buffer = new Buffer();
    table.insertionStream.subscribe(buffer.input);
    buffer.output.subscribe(this.output);

    this.rescan = function() {
        Rx.Observable.fromArray(table.records).forEach(function(x) { scanner.output.onNext (x); });
    };

    this.invalidate = function () {
        buffer.invalidate();
    };

    this.flush = function() {
        return buffer.flush();
    };

}

module.exports = TableScanner;