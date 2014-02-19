/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

function nuovoTask(){
    
    var taskType = $("#creanuovotask").val();
    var selectionimg = $("#ImgAttivattiva").val();
    
    var xhr = new XMLHttpRequest();
    
     $.jAjax({
        url: "AddTask",
        headers : {
            "taskType" : taskType,
            "selectionimg" : selectionimg
        },
        onComplete: function(xhr,status){
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                   var newTaskId = xhr.responseText;
                   alert("New task code is: " + newTaskId);
                   popolaTask();
                }
                else{
                    alert("Request was unsuccesfull: "+ xhr.status);
                }
            }
        }
    });
}
