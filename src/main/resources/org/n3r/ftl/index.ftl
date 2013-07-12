<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

<meta http-equiv="content-type" content="text/html; charset=UTF-8" />
<title>Rocket Logger</title>

<script src="http://${contextPath}/res?org/n3r/js/jquery-1.9.1.min.js" type="text/javascript"></script>
<script src="http://${contextPath}/res?org/n3r/js/jquery.jstree.js" type="text/javascript"></script>
<script src="http://${contextPath}/res?org/n3r/js/jquery.cookie.js" type="text/javascript"></script>
<script src="http://${contextPath}/res?org/n3r/js/jquery.json-2.2.min.js" type="text/javascript"></script>
<script src="http://${contextPath}/res?org/n3r/js/jquery.hotkeys.js" type="text/javascript"></script>
<script src="http://${contextPath}/res?org/n3r/js/jquery.fileDownload.js" type="text/javascript"></script>

<link rel="stylesheet" href="http://${contextPath}/res?org/n3r/css/jqtree.css"></link>
<script type="text/javascript">
var socket;
if (!window.WebSocket) {
  window.WebSocket = window.MozWebSocket;
}
if (window.WebSocket) {
  socket = new WebSocket("ws://${contextPath}/websocket");
  socket.onmessage = function(event) {
    var ta = document.getElementById('responseText');
    ta.value = ta.value + '\n' + event.data;
    ta.scrollTop = ta.scrollHeight;
  };
  socket.onopen = function(event) {
    var ta = document.getElementById('responseText');
    ta.value = "Web Socket opened!";
  };
  socket.onclose = function(event) {
    var ta = document.getElementById('responseText');
    ta.value = ta.value + "Web Socket closed"; 
  };
} else {
  alert("Your browser does not support Web Socket.");
}

function send(message) {
  if (!window.WebSocket) { return; }
  if (socket.readyState == WebSocket.OPEN) {
    socket.send(message);
  } else {
    alert("The socket is not open.");
  }
}

$(function () {
    // TO CREATE AN INSTANCE
    // select the tree container using jQuery
    $("#demo1")
        // call `.jstree` with the options object
        .jstree({
            // the `plugins` array allows you to configure the active plugins on this instance
            "plugins" : ["themes","html_data","ui","crrm","hotkeys","contextmenu"],
            // each plugin you have included can have its own config object
            "core" : { "initially_open" : [ "phtml_1" ] },
            // it makes sense to configure a plugin only if overriding the defaults
            "contextmenu" : { 
                "items": function ($node) {
                                return { "tail" : {
                                            // The item label
                                            "label"             : "Tail",
                                            // The function to execute upon a click
                                            "action"            : function (obj) { 
                                                                    var ta = document.getElementById('responseText');
                                                                    ta.value = "";
                                                                    send(obj.attr("title"));
                                                                  },
                                            },
                                         "download" : {
                                            // The item label
                                            "label"             : "Download",
                                            // The function to execute upon a click
                                            "action"            : function (obj) { 
                                                var file = obj.attr("title"); 
                                                var url = "http://${contextPath}/downloads?file=" + file + "&time=" + Date.parse(new Date());
                                                $.fileDownload(url).done(function () { alert('File download a success!'); });
                                            },
                                            }
                                        }}
            }
        })
});
</script>
</head>
<body>
<table width="100%">
    <tr>
        <td colspan="2" align="center">
            This is title! _ Rocket! (火箭)!
        </td>
    </tr>
    <tr>
        <td width="30%" valign="top">
            <div id="demo1" class="demo" style="height:100px;">
            <ul>
                <#list folderLst as folder>
                    <li id="${folder.folder}" name="${folder.folder}" title="${folder.folder}">
                        <a href="#">${folder.folder}</a>
                        <ul>
                        <#list folder.fileLst as file>
                            <li id="${file.file}" name="${file.file}" title="${file.fileCanonicalPath}">
                            <a href="#">${file.file}</a>
                            </li>
                        </#list>
                        </ul>
                    </li>
                </#list>
            </ul>
            </div>
        </td>
        <td width="70%">
            <textarea rows="40" cols="120" id="responseText"></textarea>
        </td>
    </tr>
</table>
</body>
</html>