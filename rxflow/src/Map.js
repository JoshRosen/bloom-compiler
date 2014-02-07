var Rx = require('rx');


function Map(mapFunc) {
    'use strict';
    this.input = new Rx.Subject();
    this.output = this.input.select(mapFunc);
}

module.exports = Map;