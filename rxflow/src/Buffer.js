var Rx = require('rx');


function Buffer() {
    'use strict';

    var _buffer = [];

    this.input = new Rx.Subject();
    this.input.forEach(function(x) { _buffer.push(x); });
    var _output = new Rx.Subject();
    this.output = _output;

    this.invalidate = function () {
        _buffer = [];
    };

    this.isEmpty = function() {
        return _buffer.length === 0;
    };

    this.flush = function() {
        var buffer = _buffer;
        _buffer = [];
        Rx.Observable.fromArray(buffer).forEach(function(x) { _output.onNext (x); });
        return buffer.length;
    };
}

module.exports = Buffer;