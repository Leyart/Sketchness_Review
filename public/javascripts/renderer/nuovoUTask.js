/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

function nuovoUTask(){
    
    var taskType = $("#creanuovoUTask").val();
    var selectionTask = $("#TaskAttivo").val();
    
     $.jAjax({
        url: "AddUTask",
        headers : {
            "taskType" : taskType,
            "selectionTask" : selectionTask
        },
        onComplete: function(xhr,status){
            $("#creanuovoUTask").dropkick();
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                   var newTaskId = xhr.responseText;
                   alert("New micro task code is: " + newTaskId);
                   visualizzaUTask();
                }
                else{
                    alert("Request was unsuccesfull: "+ xhr.status);
                }
            }
        }
    });
}

