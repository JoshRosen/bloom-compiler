'use strict';

global.rxflow = require('../src/index');
global.Rx = require('rx');


module.exports = {
    name: 'Buffer',
    tests : [
        {
            name: 'Push 100 ints',
            setup: function() {
                var buffer = new global.rxflow.Buffer();
                var input = new global.rxflow.InputPort(function(x) { arr.push(x); });
                var arr = [];
                buffer.output.subscribe(input);
            },
            fn: function() {
                for (var i = 1; i < 100; i++) {
                    buffer.input.onNext(i);
                }
                buffer.flush();
            }
        }
    ]
};