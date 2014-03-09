/// <reference path="../typings/rx.js/rx.d.ts" />
/// <reference path="./DataflowElement.ts" />
var Rx = require('rx');

/**
* Performs GROUP BY aggregation.
*/
var Aggregate = (function () {
    /**
    * Create a new Aggregate.
    *
    * @param keyFunction
    *      A function that extracts the key from each record.
    *      The key must be a hashable Javascript object.
    * @param aggregates
    *      A list of aggregation function classes.
    */
    function Aggregate(keyFunction, aggregates) {
        var _this = this;
        this.aggregators = [];
        this.groupKeys = [];
        this.keyToArrayIndex = {};
        this.nextArrayIndex = 0;
        /**
        * An input stream of elements to be aggregated.
        */
        this.input = Rx.Observer.create(function (x) {
            return _this.updateAggs(x);
        });
        this.output = new Rx.Subject();
        this.keyFunction = keyFunction;
        this.aggregates = aggregates;
    }
    Aggregate.prototype.createAggregators = function () {
        return this.aggregates.map(function (Cls) {
            return new Cls();
        });
    };

    Aggregate.prototype.updateAggs = function (x) {
        var key = this.keyFunction(x);
        var idx = this.keyToArrayIndex[key];
        if (idx === undefined) {
            this.keyToArrayIndex[key] = idx = this.nextArrayIndex;
            this.nextArrayIndex += 1;
            this.aggregators.push(this.createAggregators());
            this.groupKeys.push(key);
        }
        this.aggregators[idx].forEach(function (agg) {
            return agg.next(x);
        });
    };

    /**
    * Return a stream of groups and values as an Rx observable.
    */
    Aggregate.prototype.getCurrentValues = function () {
        var _this = this;
        return Rx.Observable.create(function (observer) {
            for (var i = 0; i < _this.aggregators.length; ++i) {
                observer.onNext([_this.groupKeys[i]].concat(_this.aggregators[i].map(function (agg) {
                    return agg.getValue();
                })));
            }
        });
    };

    Aggregate.prototype.flush = function () {
        var _this = this;
        this.getCurrentValues().subscribe(function () {
            return _this.output;
        });
    };

    /**
    * Reset this element by resetting aggregates to their initial values
    * and clearing all groups.
    */
    Aggregate.prototype.invalidate = function () {
        this.aggregators = [];
        this.groupKeys = [];
        this.keyToArrayIndex = {};
        this.nextArrayIndex = 0;
    };
    return Aggregate;
})();

module.exports = Aggregate;
//# sourceMappingURL=Aggregate.js.map
