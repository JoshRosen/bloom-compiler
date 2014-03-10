'use strict';

var rxflow = require('../src/index');
var vows = require('vows'),
    assert = require('assert');

vows.describe('Buffer').addBatch({
    'Test Buffer': function () {
        var buffer = new rxflow.Buffer();
        var sink = new rxflow.ObservableSink();
        var results = [];
        buffer.output.subscribe(sink.input);
        sink.output.forEach(function(x) { results.push(x); });
        buffer.input.onNext('A');
        buffer.input.onNext('B');
        assert.deepEqual(results, []);
        buffer.flush();
        assert.deepEqual(results, ['A', 'B']);
        buffer.input.onNext('C');
        buffer.flush();
        buffer.input.onNext('D');
        assert.deepEqual(results, ['A', 'B', 'C']);
        buffer.flush();
        assert.deepEqual(results, ['A', 'B', 'C', 'D']);
    },
    'Test recursive inserts are still buffered': function () {
        var buffer = new rxflow.Buffer();
        var sink = new rxflow.ObservableSink();
        buffer.output.subscribe(sink.input);
        var results = [];
        sink.output.forEach(function(x) { results.push(x); });
        sink.output.forEach(function(x) { buffer.input.onNext(10 * x); });
        buffer.input.onNext(1);
        buffer.flush();
        assert.deepEqual(results, [1]);
    }
}).export(module);
