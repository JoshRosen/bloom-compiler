var Rx = require('rx');


function HashJoin(leftKeyFunc, rightKeyFunc, buildInput) {
    'use strict';
    this.leftInput = new Rx.Subject();
    this.rightInput = new Rx.Subject();
    var _hashTable = {};
    var _buildInput;
    var _probeInput;
    var _buildKeyFunc;
    var _probeKeyFunc;
    var _resultOrderingFunction;

    if (buildInput === "left") {
        _buildInput = this.leftInput;
        _probeInput = this.rightInput;
        _buildKeyFunc = leftKeyFunc;
        _probeKeyFunc = rightKeyFunc;
        _resultOrderingFunction = function (b, p) {  return [b, p]; };
    } else if (buildInput === "right") {
        _buildInput = this.rightInput;
        _probeInput = this.leftInput;
        _buildKeyFunc = rightKeyFunc;
        _probeKeyFunc = leftKeyFunc;
        _resultOrderingFunction = function (b, p) {  return [p, b]; };
    } else {
        throw new Error("buildInput should be 'left' or 'right', not '" + buildInput + "'");
    }

    _buildInput.subscribe(function (b) {
        var key = _buildKeyFunc(b);
        if (!(key in _hashTable)) {
            _hashTable[key] = [b];
        } else {
            _hashTable[key].push(b);
        }
    });

    this.output = _probeInput.flatMap(function (p) {
        var key = _probeKeyFunc(p);
        if (!(key in _hashTable)) {
            return Rx.Observable.empty();
        } else {
            var matches = _hashTable[key];
            return Rx.Observable.fromArray(matches.map(function(b) {
                return _resultOrderingFunction(b, p);
            }));
        }
    });
}

module.exports = HashJoin;