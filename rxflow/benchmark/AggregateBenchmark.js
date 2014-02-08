'use strict';

var _ = require('underscore');
global.rxflow = require('../src/index');
global.Rx = require('rx');


// These functions are from triflow:
// https://github.com/trifacta/triflow/blob/master/benchmark/generateTestData.js

function generateStrings(cardinality, length) {
    return _.map(_.range(cardinality), function(i) {
        return _.map(_.range(length), function(j) {
            return String.fromCharCode(Math.floor(i + 2 + j + 1)+65);
        }).join('');
    });
}

function generateTestData(opt) {
    opt = _.defaults(opt || {}, {
        strColumns: 3,
        strLength: 5,
        uniqueValueRatio: .1,
        numericColumns: 3,
        numRows: 100,
        exactRatio: true
    });
    var strings = generateStrings(
        Math.floor(opt.numRows * opt.uniqueValueRatio), opt.strLength);
    return _.reduce(_.range(opt.numRows), function(accum, value, index) {
        var nextRow = _.map(_.range(opt.strColumns), function(c) {
            return strings[(index * (c + 1)) % strings.length];
        });
        accum.push(nextRow);
        return accum;
    }, []);
}

var strColumns = 10;
var testData = generateTestData({
    numRows: 10000,
    strColumns: strColumns,
    uniqueValueRation: .1
});

function Count() {
    var count = 0;
    this.getValue = function() { return count; };
    this.next = function() { ++count; };
}

module.exports = {
    name: 'Aggregate',
    tests : [
        {
            name: 'Agg test 10000',
            fn: function() {
                for (var column = 0; column < strColumns; ++column) {
                    var group = function(data) { return data[column]; };
                    var aggregate = new global.rxflow.Aggregate(group, [Count]);
                    var data = global.Rx.Observable.fromArray(testData);
                    data.subscribe(aggregate.input);
                    var results = [];
                    aggregate.getCurrentValues().forEach(function (x) { results.push(x); });
                }
            }
        }
    ]
};