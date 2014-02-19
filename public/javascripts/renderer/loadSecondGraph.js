/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
function loadSecondGraph(){
  $.jAjax({
    url: "loadSecond",
    onComplete: function(xhr,status){
      if(xhr.readyState === 4){
          if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
             var result = JSON.parse(xhr.responseText);
             drawChart2(result);
             second_graph_data = xhr.responseText;
          }
          else{
              alert("Request was unsuccesfull: "+ xhr.status);
          }
      }
    }
  });
}

var second_graph_data;

function drawChart2(result) {

var div = document.getElementById('chart_div2');
      var data = google.visualization.arrayToDataTable([]);
      data.addColumn('number', 'Images');
      data.addColumn('number', 'Users');
      for(var i= 0; i < result.length ; i++){
         data.addRow([result[i].images , result[i].users ]);
      }
      var options = {
        title: 'Number of users that have annotated X images',
        height: 400,
        width: 600,
        legend: {position: 'top', textStyle: {color: 'black', fontSize: 13}},
        hAxis: {title: 'image number', titleTextStyle: {color: 'red'}}
      };
      var chart = new google.visualization.ColumnChart(div);
      chart.draw(data, options);
}