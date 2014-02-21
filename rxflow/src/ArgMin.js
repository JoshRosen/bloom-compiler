var Rx = require('rx');
var Aggregate = require('./Aggregate');


function ArgMin(keyFunction, orderingFields, orderingFunction) {
    'use strict';

    var _this = this;

    var aggregateFunction = function () {
        var value = null;
        this.getValue = function() { return value; };
        this.next = function(x) {
            if (value === null || orderingFunction(orderingFields(x), orderingFields(value))) {
                value = x;
            }
        };
    };
    var aggregate = new Aggregate(keyFunction, [aggregateFunction]);

    this.input = aggregate.input;
    this.output = new Rx.Subject();

    this.flush = function () {
        aggregate.getCurrentValues().map(function(x) { return x[1]; }).subscribe(_this.output);
    };

    this.invalidate = aggregate.invalidate;

}
module.exports = ArgMin;