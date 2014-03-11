'use strict';

var rxflow = require('../src/index');
var vows = require('vows'),
    assert = require('assert');

vows.describe('Punctuations').addBatch({
    'Automatic flushing based on EOR punctuations': function () {
        var flushController = new rxflow.OutputPort(null);
        var table = new rxflow.Table(0);
        var tableScanner = new rxflow.TableScanner(table);
        var argMin = new rxflow.ArgMin(
            function(x) { return x[0]; },
            function(x) { return x[1]; } ,
            function(x, y) { return x <= y; }
        );

        tableScanner.output.subscribe(argMin.input);
        flushController.subscribe(table.insert);

        var observableSink = new rxflow.ObservableSink();
        var results = [];
        argMin.output.subscribe(observableSink.input);
        observableSink.output.forEach(function (x) { results.push(x); });

        table.insert.onNext([1, 2]);
        table.insert.onNext([2, 4]);
        table.insert.onNext([3, 6]);
        flushController.onNext(rxflow.punctuations.END_OF_ROUND);

        assert.deepEqual(results.sort(), [[1, 2], [2, 4], [3, 6]]);

    }
}).export(module);
