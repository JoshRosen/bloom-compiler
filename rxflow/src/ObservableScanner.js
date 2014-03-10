var Buffer = require('./Buffer');


function ObservableScanner(observable) {
    'use strict';

    var buffer = new Buffer();
    observable.subscribe(buffer.input);

    this.input = buffer.input;
    this.output = buffer.output;

    this.flush = function() {
        return buffer.flush();
    };
}

module.exports = ObservableScanner;