const dynData = {"currentTime":-1,"lastRenderedIndex":-1};
const canvasControl = {"width":635.0,"height":320.0,"videoWidth":1270.0,"videoHeight":640.0,"maxHeat":5};
window.addEventListener("load", init);

function init() {
    console.log("Initializing!");
    $.getJSON('./get-default-video-name.json', function(response){
      dynData.selectedVideoName = response.defaultVideoName;
      updateVideoName(dynData.selectedVideoName);
    });

    const canvas = $("#pathCanvas")[0];
    canvasControl.canvas=canvas;
    if (canvas.getContext) {
      const ctx = canvas.getContext("2d");
      canvasControl.ctx = ctx;
      canvasControl.ctx.willReadFrequently = true;
      canvasControl.video = $("#instoreVideo")[0];
      canvasControl.slider = $("#slider")[0];
      canvasControl.slider.max = canvasControl.video.duration;
      canvasControl.slider.step = 1;
      canvasControl.personTrackingOn = true;
      canvasControl.heatmapOn = true;
      canvasControl.maxHeat = $("#heatmapSlider")[0].value;
      $("#slider").on("input",sliderUpdate);
      $("#instoreVideo").on("timeupdate", videoTimeUpdated);
      $("#playPauseButton").on("click", playPauseVideo);
      $("#restartButton").on("click", restartVideo);
      $("#personTrackingOnOffButton").on("click", personTrackingOnOff);
      $("#heatmapOnOffButton").on("click", heatmapOnOff);
      $("#heatmapSlider").on("click",changeTemperature);
      $("#showSelectVideoButton").on("click",showSelectVideo);
      $("#selectVideoNameButton").on("click",selectVideoName);

      

      canvasControl.heatmap = h337.create({
        // only container is required, the rest will be defaults
        "container": $('#heatmap')[0],
        "maxOpacity": .6,
        "radius": 50,
        "blur": .90,
        "width": 635,
        "height": 360,
        // backgroundColor with alpha so you can see through it
        backgroundColor: 'rgba(0, 0, 58, 0.96)'
      });
      resetHeatmap();
    } else {
      canvasControl.ctx = null;
    }


  }

  function updateVideoName(videoName) {
    dynData.selectedVideoName = videoName;
    canvasControl.video.currentTime = 0;
    dynData.currentTime = 0;
    dynData.detectedPersonsCurrentIndex = 0;
    resetDetectedPersons();
    resetHeatmap();

    $("#instoreVideo").attr("src", "/get-video.mp4?videoName=" + dynData.selectedVideoName);
    $.getJSON('/get-json.json?videoName=' + dynData.selectedVideoName, function(response){
      dynData.detectedPersons = reorgLabels(response);
      updateDetectedIndex(dynData.currentTime);
    });
  }

  function showSelectVideo(e) {
    const video = canvasControl.video;
    video.pause();
    $("#playButtonImage").show();
    $("#pauseButtonImage").hide();
    $("#currentSelectedVideo").html(dynData.selectedVideoName);
    
    $.getJSON('./get-videos-list.json', function(response){
      if (response.videos&&response.videos.length>0) {
        // $("#selectVideoNameButton").prop('disabled', false);
        // $("#selectVideoNameButton").show();
        $("#selectedVideoName").val(null);
        $("#videosListOptions").empty();
        
        for (var r=0;r<response.videos.length;r++) {
          $("#videosListOptions").append("<option value='"+response.videos[r]+"'>"+response.videos[r]+"</option>");
        }
        //$("#selectVideoName").show();
      }
      if (response.isConnectionOk) {
        $("#onlineImage").show();
        $("#offlineImage").hide();
      } else {
        $("#onlineImage").hide();
        $("#offlineImage").show();
      }
    });
  }

  function selectVideoName(e) {
    if (!$("#selectedVideoName")[0].value) {
      return;
    }
    //$("#selectVideoName").hide();
    dynData.selectedVideoName = $("#selectedVideoName")[0].value;
    updateVideoName(dynData.selectedVideoName);
  }

  function sliderUpdate(e) {
    const video = canvasControl.video;
    const slider = canvasControl.slider;
    var changed = false;
    for(var r=0;r<video.seekable.length;r++) {
      if (slider.value>=video.seekable.start(r) && slider.value<=video.seekable.end(r)) {
        video.currentTime = slider.value;
        changed = true;
        break;
      }
    }
    if (!changed)
      slider.value = video.currentTime;
  }

  function restartVideo(e) {
    canvasControl.video.currentTime = 0;
  }

  function addHeatmapData() {
    
  }

  function playPauseVideo(e) {
    const video = canvasControl.video;
    console.log("playPauseVideo:" + video.paused);
    if (video.paused) {
      $("#playButtonImage").hide();
      $("#pauseButtonImage").show();
      video.play();
    } else {
      $("#playButtonImage").show();
      $("#pauseButtonImage").hide();
      video.pause();
    }
  }
  function personTrackingOnOff(e) {
    if (canvasControl.personTrackingOn) {
      $("#personTrackingOnButtonImage").hide();
      $("#personTrackingOffButtonImage").show();
      $("#selectPersonIdsButton").prop('disabled', true);
      canvasControl.personTrackingOn = false;
    } else {
      $("#personTrackingOnButtonImage").show();
      $("#personTrackingOffButtonImage").hide();
      $("#selectPersonIdsButton").prop('disabled', false);
      canvasControl.personTrackingOn = true;
    }
    renderDetectedPersons(dynData.detectedPersonsCurrentIndex);
  }
  function heatmapOnOff(e) {
    if (canvasControl.heatmapOn) {
      $("#heatmapOnButtonImage").hide();
      $("#heatmapOffButtonImage").show();
      $("#heatmapSlider").prop('disabled', true);
      canvasControl.heatmapOn = false;
      resetHeatmap();
    } else {
      $("#heatmapOnButtonImage").show();
      $("#heatmapOffButtonImage").hide();
      $("#heatmapSlider").prop('disabled', false);;
      canvasControl.heatmapOn = true;
      resetHeatmap();
    }
  }
  function changeTemperature(e) {
    setTemperature();
    console.log("Changed the temperature to "+canvasControl.maxHeat);
  }
  function setTemperature() {
    canvasControl.maxHeat = $("#heatmapSlider")[0].value;
    canvasControl.heatmap.setDataMax(canvasControl.maxHeat);
  }
  function videoTimeUpdated(event) {
    canvasControl.currentTime = event.target.currentTime*1000;
    updateDetectedIndex(canvasControl.currentTime);
  }

  function animate(event) {

  }

  function updateDetectedIndex(currentTime) {
    if (dynData.currentTime!=currentTime) {
      var direction = dynData.currentTime>currentTime?-1:1;
      console.log("direction:" + direction);
      var maxCx = dynData.detectedPersons.length;
      console.log("maxCx:" + maxCx);
      var currentIndex;

      for(currentIndex = dynData.detectedPersonsCurrentIndex;currentIndex>-1 && currentIndex<maxCx; currentIndex+=direction) {
        detectedPersonsCurrentTime = dynData.detectedPersons[currentIndex][0].timestamp;
        siblingDetectedPersonsCurrentTime = ( ((currentIndex+direction)>-1) && ((currentIndex+direction)<maxCx) )?dynData.detectedPersons[currentIndex+direction][0].timestamp:detectedPersonsCurrentTime;
        if ( ( detectedPersonsCurrentTime<=currentTime && siblingDetectedPersonsCurrentTime>=currentTime) ||
             ( detectedPersonsCurrentTime<=currentTime && siblingDetectedPersonsCurrentTime>=currentTime) ) {

            console.log("detectedPersonsCurrentTime:" + detectedPersonsCurrentTime);
            console.log("currentTime:" + currentTime);
            console.log("siblingDetectedPersonsCurrentTime:" + siblingDetectedPersonsCurrentTime);
            
            break;
        }
      }
      dynData.detectedPersonsCurrentIndex = currentIndex<0?0:(currentIndex>=maxCx?maxCx-1:currentIndex);
      console.log("CurrentIndex: " + currentIndex);
      console.log("detectedPersons - CurrentIndex: " + dynData.detectedPersonsCurrentIndex);
      console.log("detectedPersons - CurrentTime:" + dynData.detectedPersons[dynData.detectedPersonsCurrentIndex][0].timestamp);
      dynData.currentTime = currentTime;
      canvasControl.slider.value = currentTime/1000;

      if (currentTime==0) {
        resetHeatmap();
      }

      renderDetectedPersons(dynData.detectedPersonsCurrentIndex)
    }
  }

  function resetHeatmap() {
    canvasControl.heatmap.setData({"data":[{"x":0,"y":0,"value":0}],"max":canvasControl.maxHeat,"min":0});
  }
  function resetDetectedPersons() {
    const ctx = canvasControl.ctx;
    ctx.clearRect(0, 0, canvasControl.videoWidth, canvasControl.videoHeight);
  }
  function renderDetectedPersons(newIndex) {
    //if (dynData.lastRenderedIndex!=newIndex) {
      const ctx = canvasControl.ctx;
      ctx.clearRect(0, 0, canvasControl.videoWidth, canvasControl.videoHeight);
      var selectedPersonsToTrack = $("#selectedPersonsToTrack")[0];
      var personIdsToTrack = [];
      var trackAllIds = (selectedPersonsToTrack.selectedIndex<1);
      if (!trackAllIds) {
        var selectedOptions = selectedPersonsToTrack.selectedOptions;
        for(var opIdx=0;opIdx < selectedOptions.length; opIdx++) {
          option= selectedOptions[opIdx];
          var personName = "P:"+(option.value);
          personIdsToTrack[personName] = true;
        }
      }

      var persons = dynData.detectedPersons[newIndex];
      for(var pIndex=0;pIndex<persons.length;pIndex++) {
          var person = persons[pIndex].person;
          const personLabel = "P:"+person.index;          
          if (person.boundingBox && (trackAllIds || personIdsToTrack[personLabel])) {
            var x = person.boundingBox.left*canvasControl.width;
            var y = person.boundingBox.top*canvasControl.height;
            var w = person.boundingBox.width*canvasControl.width;
            var h = person.boundingBox.height*canvasControl.height;

            if (canvasControl.personTrackingOn) {
              //ctx.fillStyle = "rgb(200 0 0 / 50%)"; 
              ctx.strokeStyle = "rgb(0 200 0)"; 
              //ctx.fillRect(x, y, w, h); 
              ctx.strokeRect(x, y, w, h); 
              ctx.font = "16px Arial";
              ctx.fillStyle = "rgb(255 255 255)";
              const text = ctx.measureText(personLabel)
              ctx.strokeText(personLabel, x+2, y+text.fontBoundingBoxAscent);
              ctx.fillText(personLabel, x+2, y+text.fontBoundingBoxAscent);
            }

            if(canvasControl.heatmapOn) {
              var hmX = x + w/2;
              var hmY = y + h/2;
  
              setTemperature();
              
              canvasControl.heatmap.addData({ "x": hmX, "y": hmY, "value": 1 });
            }
          }
      }
      dynData.lastRenderedIndex=newIndex;
    //}
  }

  function getGraphId(graphType,index,subindex,tail) {
    return graphType+"_idx"+index+"_sbx"+subindex+(tail?tail:"");
  }

  function reorgLabels(data) {
    var labelsPerTimestamp = [];
    var timestamp = 0;
    var tmpList = [];
    var personIds = [];
    var selectedPersonsToTrack = $("#selectedPersonsToTrack");
    data.forEach(el => {

      if (timestamp==el.timestamp) 
        tmpList.push(el);
      else {
        if (tmpList.length>0) {
          labelsPerTimestamp.push(tmpList);
          tmpList.forEach(item=>{
            var person = item.person;
            var personName = "P:"+(person.index);
            if (!personIds[personName]) {
              personIds[personName] = true;
              selectedPersonsToTrack.append("<option value='"+person.index+"'>"+personName+"</option>");
            }
          });
          tmpList = [];
        }
        tmpList.push(el);
        timestamp = el.timestamp;
      } 
    });
    return labelsPerTimestamp;

  }
