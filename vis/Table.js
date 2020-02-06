/*
 * Copyright 2020 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
if (!visualisations) {
    var visualisations = {};
}
//IIFE to prvide shared scope for sharing state and constants between the controller 
//object and each grid cell object instance
(function(){

    var commonFunctions = visualisations.commonFunctions;
    var commonConstants = visualisations.commonConstants;

    visualisations.Table = function() {


        var d3 = window.d3;
        this.element = window.document.createElement("div");
        var grid = new visualisations.GenericGrid(this.element);

        this.start = function() {
            //TODO do we need this?
            svg.selectAll(".text-value")
                .remove();

        }

        //Method to allow the grid to call back in to get new instances for each cell
        this.getInstance = function(containerNode) {
            return new visualisations.Table.Visualisation(containerNode);
        };

        //Public method for setting the data on the visualisation(s) as a whole
        //This is the entry point from Stroom
        this.setData = function(context, settings, data) {

            if (data && data !==null) {
                //#########################################################
                //Perform any visualisation specific data manipulation here
                //#########################################################

                if (settings) {

                    settings.requiresLegend = false;

                    //Inspect settings to determine which axes to synch, if any.
                    //Change the settings property(s) used according to the vis
                    var synchedFields = [];
                    var visSpecificState = {};

                    //Get grid to construct the grid cells and for each one call back into a 
                    //new instance of this to build the visualisation in the cell
                    //The last array arg allows you to synchronise the scales of fields
                    grid.buildGrid(context, settings, data, this, commonConstants.transitionDuration, synchedFields, visSpecificState);
                }
            }
        };

        this.resize = function() {
            grid.resize();
        };

        this.getLegendKeyField = function() {
            return 0;
        };

    };

    //This is the content of the visualisation inside the containerNode
    //One instance will be created per grid cell
    visualisations.Table.Visualisation = function(containerNode) {

        var element = containerNode;
        var margins = commonConstants.margins();

        var width;
        var height;
        ;
        // Add the series data.
        var seriesContainer;
        var visData;
        var visSettings;
        var visContext;

        var svg = d3.select(element).append("svg:svg");

        var canvas = svg.append("svg:g");

        var columns = [
            { head: 'Movie title', cl: 'title',
              html: function(row) { return row.title; } },
            { head: 'Year', cl: 'center',
              html: function(row) { return row.year; } },
            { head: 'Length', cl: 'center',
              html: function(row) { return row.length; } },
            { head: 'Budget', cl: 'num',
              html: function(row) { return row.budget; } },
            { head: 'Rating', cl: 'num',
              html: function(row) { return row.rating; } }
        ];
        
        var movies = [
            { title: "The Godfather", year: 1972, length: 175, budget: 6000000, rating: 9.1 },
            { title: "The Shawshank Redemption", year: 1994, length: 142, budget: 25000000, rating: 9.1 },
            { title: "The Lord of the Rings: The Return of the King", year: 2003, length: 251, budget: 94000000, rating: 9 },
            { title: "The Godfather: Part II", year: 1974, length: 200, budget: 13000000, rating: 8.9 },
            { title: "Shichinin no samurai", year: 1954, length: 206, budget: 500000, rating: 8.9 },
            { title: "Buono, il brutto, il cattivo, Il", year: 1966, length: 180, budget: 1200000, rating: 8.8 },
            { title: "Casablanca", year: 1942, length: 102, budget: 950000, rating: 8.8 },
            { title: "The Lord of the Rings: The Fellowship of the Ring", year: 2001, length: 208, budget: 93000000, rating: 8.8 },
            { title: "The Lord of the Rings: The Two Towers", year: 2002, length: 223, budget: 94000000, rating: 8.8 },
            { title: "Pulp Fiction", year: 1994, length: 168, budget: 8000000, rating: 8.8 }
        ];
    

        
        var div = svg.append('foreignObject').attr('x','0').attr('y','0').attr('width','1200').attr('height','400')
        .append('xhtml:div').attr('xmlns','http://www.w3.org/1999/xhtml').attr('id','tablediv101')
        
        
        var table = div.append('table')
        
        var thead = table.append('thead');
        var tbody = table.append('tbody');
               
        
        // var table = div.append('xhtml:table');

        //Public entry point for the Grid to call back in to set the cell level data on the cell level 
        //visualisation instance.
        //data will only contain the branch of the tree for this cell
        this.setDataInsideGrid = function(context, settings, data) {
            visData = data;
            visContext = context;
            visSettings = settings;
            update(0);
        };

        var update = function(duration) {
            if (visData) {
                width = commonFunctions.gridAwareWidthFunc(true, containerNode, element);
                height = commonFunctions.gridAwareHeightFunc(true, containerNode, element);
                fullWidth = commonFunctions.gridAwareWidthFunc(false, containerNode, element);
                fullHeight = commonFunctions.gridAwareHeightFunc(false, containerNode, element);


                thead.selectAll('th')
                .data(columns).enter()
                .append('th')
                .attr('class', function(d){return d.cl})
                .text(function(d){return d.head;});

                tbody.selectAll('tr')
                .data(movies).enter()
                .append('tr')
                .selectAll('td')
                .data(function(row, i) {
                    return columns.map(function(c) {
                        // compute cell values for this specific row
                        var cell = {};
                        d3.keys(c).forEach(function(k) {
                            cell[k] = typeof c[k] == 'function' ? c[k](row,i) : c[k];
                        });
                        return cell;
                    });
                }).enter()
                .append('td')
                .html(function(d){return d.html;})
                .attr('class', function(d){return d.cl;});
                    }
                };

        this.getColourScale = function(){
            return null;
        };

        this.teardown = function() {

        };
    };

}());

