'use strict';

var rxflow = require("../src/index");
var vows = require('vows'),
    assert = require('assert');


function singleGroup() { return null; }
function firstCol(x) { return x[0]; }
function firstTwoCols(x) { return [x[0], x[1]]; }



function Count() {
    var count = 0;
    this.getValue = function() { return count; };
    this.next = function() { ++count; };
}

function SumThirdCol() {
    var sum = 0;
    this.getValue = function() { return sum; };
    this.next = function(x) { sum += x[2]; };
}

vows.describe('Aggregate').addBatch({
    'Test single aggregate': function() {
        var aggregate = new rxflow.Aggregate(singleGroup, [Count]);
        aggregate.input.onNext(['a']);
        aggregate.input.onNext(['b']);
        aggregate.input.onNext(['c']);
        var results = [];
        aggregate.getCurrentValues().forEach(function (x) { results.push(x); });
        assert.deepEqual(results, [[null, 3]]);
    },
    'Test single group': function() {
        var aggregate = new rxflow.Aggregate(firstCol, []);
        aggregate.input.onNext(['a']);
        aggregate.input.onNext(['b']);
        aggregate.input.onNext(['c']);
        var results = [];
        aggregate.getCurrentValues().forEach(function (x) { results.push(x); });
        assert.deepEqual(results.sort(), [['a'], ['b'], ['c']]);
    },
    'Test count with single group': function() {
        var aggregate = new rxflow.Aggregate(firstCol, [Count]);
        aggregate.input.onNext(['a']);
        aggregate.input.onNext(['b']);
        aggregate.input.onNext(['b']);
        var results = [];
        aggregate.getCurrentValues().forEach(function (x) { results.push(x); });
        assert.deepEqual(results.sort(), [['a', 1], ['b', 2]]);
    },
    'Test multiple aggs/multiple groups': function() {
        var aggregate = new rxflow.Aggregate(firstTwoCols, [Count, SumThirdCol]);
        aggregate.input.onNext(['a', 'x', 1]);
        aggregate.input.onNext(['a', 'y', 1]);
        aggregate.input.onNext(['a', 'y', 3]);
        aggregate.input.onNext(['b', 'x', 1]);
        aggregate.input.onNext(['b', 'x', 2]);
        aggregate.input.onNext(['b', 'x', 3]);
        var results = [];
        aggregate.getCurrentValues().forEach(function (x) { results.push(x); });
        assert.deepEqual(results.sort(), [[['a', 'x'], 1, 1], [['a', 'y'], 2, 4], [['b', 'x'], 3, 6]]);
    }
}).export(module);
