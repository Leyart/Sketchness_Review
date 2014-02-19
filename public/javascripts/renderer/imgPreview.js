function imgPreview(url, idPreview){
    var taskImage
       ,canvas = document.getElementById("draws")
       ,ctx = canvas.getContext("2d")
       ,taskCanvas = document.getElementById("task")
       ,taskContext = taskCanvas.getContext("2d")
       ,graph1 = document.getElementById("chart_div1")
       ,graph2 = document.getElementById("chart_div2")
       ,viewport = document.getElementById("viewport");

        $(graph1).hide();
        $(graph2).hide();
        $(viewport).show();


        taskImage=new Image();
        taskImage.src=url;
        $("#ImgPreview").val(idPreview);

        taskImage.onload = function() {
                    taskContext.save();
                    taskContext.beginPath();
                    taskCanvas.width=this.width;
                    taskCanvas.height=this.height;
                    canvas.width=this.width;
                    canvas.height=this.height;
                    taskContext.drawImage(taskImage,0,0,this.width,this.height);
                    taskContext.restore();
                    //Clear the mask canvas
                    var maskCanvas = document.getElementById("mask");
                    var maskContext = maskCanvas.getContext("2d");
                    maskContext.clearRect(0,0,maskCanvas.width,maskCanvas.height);
                };

}