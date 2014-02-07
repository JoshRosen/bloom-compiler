var Rx = require('rx');


function Map(mapFunc) {
    'use strict';
    var _output = new Rx.Subject();
    this.output = _output;
    this.input = new Rx.Observer.create(
        function (x) {
            _output.onNext(mapFunc(x));
        }
    );
}

module.exports = Map;