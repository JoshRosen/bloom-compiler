var Rx = require('rx');

/**
 * Performs GROUP BY aggregation.
 *
 * @param keyFunction
 *      A function that extracts the key from each record.
 *      The key must be a hashable Javascript object.
 * @param {Array.<AggregationFunction>} aggregates
 *      A list of aggregation function classes.
 * @constructor
 */
function Aggregate(keyFunction, aggregates) {
    'use strict';

    var _aggregators = [];
    var _groupKeys = [];
    var _keyToArrayIndex = {};
    var _nextArrayIndex = 0;

    function createAggregators() {
        return aggregates.map(function(Cls) { return new Cls(); });
    }

    function updateAggs(x) {
        var key = keyFunction(x);
        var idx = _keyToArrayIndex[key];
        if (idx === undefined) {
            _keyToArrayIndex[key] = idx = _nextArrayIndex;
            _nextArrayIndex += 1;
            _aggregators.push(createAggregators());
            _groupKeys.push(key);
        }
        _aggregators[idx].forEach(function(agg) { agg.next(x); });
    }

    /**
     * An input stream of elements to be aggregated.
     * @type {Rx.Observer}
     */
    this.input = Rx.Observer.create(updateAggs);

    /**
     * Return a stream of groups and values as an Rx observable.
     */
    this.getCurrentValues = function() {
        function extractValue(agg) { return agg.getValue(); }
        return Rx.Observable.create(function(observer) {
            for (var i = 0; i < _aggregators.length; ++i) {
                observer.onNext([_groupKeys[i]].concat(_aggregators[i].map(extractValue)));
            }
            observer.onCompleted();
        });
    };

    /**
     * Reset this element by resetting aggregates to their initial values
     * and clearing all groups.
     */
    this.reset = function() {
        _aggregators = [];
        _groupKeys = [];
        _keyToArrayIndex = {};
        _nextArrayIndex = 0;
    };
}


/* jshint ignore:start */
/**
 * An aggregation function.  Instances maintain internal
 * state for computing aggregates, and expose methods to
 * retrieve the current aggregate value.
 * @interface
 * @constructor
 */
function AggregationFunction() {
    'use strict';

    /**
     * Returns the current value of the aggregate.
     */
    this.getValue = function() {};

    /**
     * Update the aggregate with a new value.
     */
    this.next = function(val) {};
}
/* jshint ignore:end */

module.exports = Aggregate;