/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
function popolaTask(){
    var selectionTask = $("#selectionTask")
        ,option
        ,ids
        ,tasks
        ,result;

    selectionTask.children().remove();

    selectionTask.append("<option value='' selected disabled>Select Task</option>");
    $('.customSelect').dropkick();

    $.jAjax({
        url: "TaskSelection",
        onComplete: function(xhr,status){
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                    result = JSON.parse(xhr.responseText);

                    if(result.check == "true"){
                        tasks = result.task[0];
                        $.each(tasks, function(i,d){

                                option=document.createElement("option");
                                option.value= d.id.substring(1, d.id.length -1);
                                option.text= d.id.substring(1, d.id.length -1) + " ( " + d.taskType.substring(1, d.taskType.length -1) + " )";
                                selectionTask.append(option);

                        });
                    }
                    $('.customSelect').dropkick("refresh");
                }
                else{
                    alert("Request was unsuccesfull: "+ xhr.status);
                }
            }
        }
    });
}
