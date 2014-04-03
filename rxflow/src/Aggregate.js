var __extends = this.__extends || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    __.prototype = b.prototype;
    d.prototype = new __();
};
var DataflowElement = require('./DataflowElement');
var InputPort = require('./InputPort');
var OutputPort = require('./OutputPort');

/**
* Performs GROUP BY aggregation.
*/
var Aggregate = (function (_super) {
    __extends(Aggregate, _super);
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
        _super.call(this);
        this.aggregators = [];
        this.groupKeys = [];
        this.keyToArrayIndex = {};
        this.nextArrayIndex = 0;
        /**
        * An input stream of elements to be aggregated.
        */
        this.input = new InputPort(function (x) {
            return _this.updateAggs(x);
        }, this);
        this.output = new OutputPort(this);
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

    Aggregate.prototype.handleEndOfRound = function () {
        for (var i = 0; i < this.aggregators.length; ++i) {
            this.output.onNext([this.groupKeys[i]].concat(this.aggregators[i].map(function (agg) {
                return agg.getValue();
            })));
        }
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
})(DataflowElement);

module.exports = Aggregate;
//# sourceMappingURL=Aggregate.js.map
