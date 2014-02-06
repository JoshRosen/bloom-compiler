var Rx = require('rx');


function Map(mapFunc) {
    this.input = new Rx.Subject();
    this.output = this.input.select(mapFunc);
}

module.exports = Map