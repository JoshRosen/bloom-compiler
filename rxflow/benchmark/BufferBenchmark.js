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
                var numbers = global.Rx.Observable.range(0, 100);
                var arr = [];
                buffer.output.subscribe(function(x) { arr.push(x); });
            },
            fn: function() {
                numbers.subscribe(buffer.input);
                buffer.flush();
            }
        }
    ]
};