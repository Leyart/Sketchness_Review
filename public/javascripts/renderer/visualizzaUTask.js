/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

function  visualizzaUTask(){
    
    var idselected = $("#selectionTask").val()
        ,uTaskContainer = $("#uTaskContainer");
    
    uTaskContainer.find(".tableRow").remove();
    
    if(idselected === "" ){
        alert("Please select a Task!");
        return;
    }
    $.jAjax({
        url: "WebTaskAjax",
        headers : {"selected" : idselected},
        onComplete: function(xhr,status){
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                    var result = JSON.parse(xhr.responseText)[0];
                    var stringaTask= result.uTasks;
                    var aperto = result.status.substring(1, result.status.length -1);
                    
                    if(stringaTask[0].utask === "empty"){
                        $("#noMicrotaskLabel").show();
                    }
                    else{
                        $("#noMicrotaskLabel").hide();

                        $.each(stringaTask, function(i,d){
                            var tableRow = "<tr class='tableRow'>"+
                                                "<td>"+ d.id.substring(1,d.id.length-1) +"</td>"+
                                                "<td>"+ d.taskType.substring(1,d.taskType.length-1) +"</td>"+
                                                "<td>"+ d.status.substring(1,d.status.length-1) +"</td>"+
                                            "</tr>";
                            uTaskContainer.append(tableRow);
                        });
                        
                        if(aperto === "1"){
                            $("#closeTaskButton").hide();   //for the review
                        //    $("#closeTaskButton").show();
                            $("#closedTaskLabel").hide();
                        }
                        else{
                            $("#closeTaskButton").hide();
                            $("#closedTaskLabel").show();
                        }
                    }//fine else
                    
                    $("#TaskAttivo").val(idselected);
                    $("#risultatitask").show();
                }
                else{
                    alert("Request was unsuccesfull: "+ xhr.status);
                }
            }
        }
    });
}
