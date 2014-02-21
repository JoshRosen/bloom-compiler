'use strict';

var rxflow = require('../src/index');
var vows = require('vows'),
    assert = require('assert');

vows.describe('Table').addBatch({
    'Test primary key constraint violation': function () {
        var table = new rxflow.Table(0);
        table.insert.onNext([1, 'A']);
        assert.throws(function() { table.insert.onNext([1, 'B']); }, Error);
    },
    'Test composite keys and values': function () {
        var table = new rxflow.Table(1);
        table.insert.onNext([1, 'A', 'B', 2]);
        assert.throws(function() { table.insert.onNext([1, 'A', 'B', 3]); }, Error);
    }
}).export(module);
