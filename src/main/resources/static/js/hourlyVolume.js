// Render the graph of volume vs. hour of the day
var margin = { top: 20, right: 20, bottom: 70, left: 80 },
    width = 600 - margin.left - margin.right,
    height = 300 - margin.top - margin.bottom;

var x = d3.scale.ordinal().rangeRoundBands([0, width], .05);

var y = d3.scale.linear().range([height, 0]);

var xAxis = d3.svg.axis()
    .scale(x)
    .orient("bottom")
    .ticks(24);

var yAxis = d3.svg.axis()
    .scale(y)
    .orient("left")
    .ticks(10);

var svg = d3.select(".hourly_volume")
    .attr("width", width + margin.left + margin.right)
    .attr("height", height + margin.top + margin.bottom)
  .append("g")
    .attr("transform", 
          "translate(" + margin.left + "," + margin.top + ")");

// Call this when data for this chart arrives via the websocket
// Arg is the "rows" attribute of that JSON
function buildChart (data) {

    data.forEach(function(d) {
        d.date = d.hour_of_day;
        d.value = +d.sum;
    });
	
  x.domain(data.map(function(d) { return d.hour_of_day; }));
  y.domain([0, d3.max(data, function(d) { return d.sum; })]);

  svg.append("g")
      .attr("class", "x axis")
      .attr("transform", "translate(0," + height + ")")
      .call(xAxis)
    .selectAll("text")
      .style("text-anchor", "end")
      .attr("dx", "-.8em")
      .attr("dy", "-.55em")
      .attr("transform", "rotate(-90)" );

  svg.append("g")
      .attr("class", "y axis")
      .call(yAxis)
    .append("text")
      .attr("transform", "rotate(-90)")
      .attr("y", -50)
      .attr("dy", ".71em")
      .style("text-anchor", "end")
      //.style("alignment-baseline", "central")
      .text("Value (BTC, thousands)");

  svg.selectAll("bar")
      .data(data)
    .enter().append("rect")
      .style("fill", "#EB973D") // Bitcoin logo color
      .attr("x", function(d) { return x(d.hour_of_day); })
      .attr("width", x.rangeBand())
      .attr("y", function(d) { return y(d.sum); })
      .attr("height", function(d) { return height - y(d.sum); });

}

