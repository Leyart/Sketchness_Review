/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
function loadFirstGraph(){
  $.jAjax({
    url: "loadFirst",
    onComplete: function(xhr,status){
      if(xhr.readyState === 4){
          if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
             var result = JSON.parse(xhr.responseText);
             drawChart(result);
             first_graph_data = xhr.responseText;

          }
          else{
              alert("Request was unsuccesfull: "+ xhr.status);
          }
      }
    }
  });
}

var first_graph_data;

function drawChart(result) {
     var div = document.getElementById('chart_div1');
     var data = google.visualization.arrayToDataTable([]);
     data.addColumn('number', 'Number od annotations ');
     data.addColumn('number', 'Number of images ');
     for(var i= 0; i < result.length ; i++){
        data.addRow([result[i].annotations , result[i].occurence ]);
     }
     var options = {
       title: 'Number of images with X annotations',
       height: 400,
       width: 600,
       legend: {position: 'top', textStyle: {color: 'black', fontSize: 13}},
       hAxis: {title: 'annotation number', titleTextStyle: {color: 'red'}}
     };
     var chart = new google.visualization.ColumnChart(div);
     chart.draw(data, options);
}
