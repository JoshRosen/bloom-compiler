var Rx = require('rx');


function Buffer() {
    'use strict';

    var  _buffer = [];

    this.input = new Rx.Subject();
    this.input.forEach(function(x) { _buffer.push(x); });
    var _output = new Rx.Subject();
    this.output = _output;

    this.flush = function() {
        Rx.Observable.fromArray(_buffer).forEach(function(x) { _output.onNext (x); });
        _buffer = [];
    };
}

module.exports = Buffer;