/// <reference path="../typings/rx.js/rx.d.ts" />
/**
* Base class for RxFlow dataflow elements.
*/
var DataflowElement = (function () {
    function DataflowElement() {
    }
    DataflowElement.prototype.invalidate = function () {
        // This space intentionally left empty
    };
    return DataflowElement;
})();

module.exports = DataflowElement;
//# sourceMappingURL=DataflowElement.js.map
