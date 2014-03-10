var rxflow = require("../src/index");
var vows = require('vows'),
    assert = require('assert');

vows.describe('HashJoin').addBatch({
    'Test HashJoin': function () {
        'use strict';
        var join = new rxflow.HashJoin(
            function(x) { return x[1]; },
            function(x) { return x[0]; },
            "left"
        );
        var joinResults = [];
        var sink = new rxflow.ObservableSink();
        join.output.subscribe(sink.input);
        sink.output.forEach(function(x) { joinResults.push(x); });
        join.leftInput.onNext([1, 2]);
        join.leftInput.onNext([1, 3]);
        join.rightInput.onNext([2, 4]);
        join.rightInput.onNext([3, 1]);
        join.rightInput.onNext([5, 5]);
        var expected = [
            [[1, 2], [2, 4]],
            [[1, 3], [3, 1]]
        ].sort();
        assert.deepEqual(expected.sort(), joinResults.sort());
    }
}).export(module);
