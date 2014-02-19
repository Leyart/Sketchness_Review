/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

function visualizzaImg(){
    var ids = $("selection").val();
    var ida = $("attivo").val();
    if(ida !== ""){
        $(ida).hide();
    }
    $(ids).show();
    $("attivo").val(ids);
}

function updateSize(evt){

}

function displayGraphs(){
    var graph1 = document.getElementById("chart_div1");
    var graph2 = document.getElementById("chart_div2");
    var canvas = document.getElementById("viewport");

    $(canvas).hide();
    $(graph1).show();
    $(graph2).show();

}
