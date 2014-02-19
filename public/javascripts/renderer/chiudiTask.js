/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

function chiudiTask(){
    
    var selectionTask = $("#TaskAttivo").val();
    var xhr = new XMLHttpRequest();
    
    $.jAjax({
        url: "CloseTask",
        headers : {"selectionTask" : selectionTask},
        onComplete: function(xhr,status){
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                   if(xhr.responseText === "ok"){
                        alert("Task closed succesfully");
                   }
                   visualizzaUTask();
                }
                else{
                    alert("Request was unsuccesfull: "+ xhr.status);
                }
            }
        }  
    });
}
