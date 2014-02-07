'use strict';

global.rxflow = require('../src/index');
global.Rx = require('rx');


module.exports = {
    name: 'Map',
    tests : [
        {
            name: 'Multiply 100000 ints directly',
            setup: function() {
                var fn = function(x) { return 2 * x; };
                var arr = [];
            },
            fn: function() {
                for (var i = 0; i < 100000; i++) {
                    arr.push(fn(i));
                }
            }
        },
        {
            name: 'Multiply 100000 ints with rxflow.Map',
            setup: function() {
                var map = new global.rxflow.Map(function(x) { return 2 * x; });
                var arr = [];
                map.output.subscribe(function(x) { arr.push(x); });
            },
            fn: function() {
                for (var i = 0; i < 100000; i++) {
                    map.input.onNext(i);
                }
            }
        }
    ]
};