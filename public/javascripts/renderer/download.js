function download1(){

    $.jAjax({
        url: "downloadStats1",
        onComplete: function(xhr,status){
          if(xhr.readyState === 4){
              if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){

                  var result = xhr.responseText;
                  var blob = new Blob([result], {
                      type: "text/plain;charset=utf-8;",
                  });
                  saveAs(blob, "userData.json");
              }
              else{
                  alert("Request was unsuccesfull: "+ xhr.status);
              }
          }
        }
      });





}

function download2(){

    $.jAjax({
        url: "downloadStats2",
        onComplete: function(xhr,status){
          if(xhr.readyState === 4){
              if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){

                  var result = xhr.responseText;
                  var blob = new Blob([result], {
                      type: "text/plain;charset=utf-8;",
                  });
                  saveAs(blob, "imageData.json");
              }
              else{
                  alert("Request was unsuccesfull: "+ xhr.status);
              }
          }
        }
      });





}

function downloadgraph1(){

    var blob = new Blob([first_graph_data], {
      type: "text/plain;charset=utf-8;",
    });
    saveAs(blob, "firstGraph.json");

}

function downloadgraph2(){

    var blob = new Blob([second_graph_data], {
      type: "text/plain;charset=utf-8;",
    });
    saveAs(blob, "secondGraph.json");

}