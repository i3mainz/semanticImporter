function getMappingSchemas(identifier,tableheader,table){
	  $.ajax({
       url: 'rest/service/getMappingSchemas',
       type: 'GET',         // HTTP-Methode, hier: POST
       processData: false,
       contentType: false,
       success: function(data) { 
       	result=""
       	for(mapping in data){
			result+="<option value=\""+data[mapping]+"\">"+mapping+"</option>"
		}
		$("#"+identifier).html(result)
		 var opt = $("#"+identifier+" option").sort(function (a,b) { return a.text.toUpperCase().localeCompare(b.text.toUpperCase()) });
    	$("#"+identifier).html(opt);
		getMappingSchema($("#"+identifier).val(),identifier,tableheader,table)
       }
    });
}

function mappingSchemaToTable(identifier,tableheader,table){
	getMappingSchema($("#"+identifier).val(),identifier,tableheader,table)
}

function getMappingSchema(schemaname,identifier,tableheader,table){
	  $.ajax({
       url: 'rest/service/getMappingSchema/'+schemaname,
       type: 'GET',         // HTTP-Methode, hier: POST
       processData: false,
       contentType: false,
       success: function(data) { 
       	   mappingSchemaReader('rest/service/getMappingSchema/'+schemaname,data,tableheader,table)
       }
    });
}

var output=""
var classes=""

function processColumns(columnhead,xml,depth){
	console.log("Depth "+depth);
	console.log()
	if(xml.tagName=="column" || xml.tagName=="addcolumn" || xml.tagName=="classmapping" || xml.tagName=="rootclass"){
        output+="<tr>"
        if((typeof $(xml).attr("name") !== 'undefined')){
        	output+="<td align=\"center\" style=\"color:red\">"+columnhead+$(xml).attr("name")+"</td>"
        }else if(xml.tagName=="addcolumn"){
        	output+="<td align=\"center\" style=\"color:green\">Additional column</td>"
        }else if(xml.tagName=="classmapping"){
        	output+="<td align=\"center\" style=\"color:green\">Additional class</td>"
        }else if(xml.tagName=="rootclass"){
        	output+="<td align=\"center\" style=\"color:green\">Root class</td>"
        }
        if((typeof $(xml).attr("prop") !== 'undefined')){
        	output+="<td align=\"center\">"+$(xml).attr("prop")+"</td>"
        }else{
        	output+="<td align=\"center\">class</td>"
        }
        output+="<td align=\"center\">"
        if((typeof $(xml).attr("propiri") !== 'undefined')){
        	output+="<a href=\""+$(xml).attr("propiri")+"\" target=\"_blank\" >"+((typeof $(xml).attr("propiri") !== 'undefined')?$(xml).attr("propiri").substring($(xml).attr("propiri").lastIndexOf('/')+1):"")+"</a>"	
        }else if((typeof $(xml).attr("class") !== 'undefined')){
        	output+="<a href=\""+$(xml).attr("class")+"\" target=\"_blank\" >"+((typeof $(xml).attr("class") !== 'undefined')?$(xml).attr("class").substring($(xml).attr("class").lastIndexOf('/')+1):"")+"</a>"
        }   
        if($(xml).children("proplabel").length>0 || $(xml).children("clslabel").length>0){
    		output+="<table width=\"100%\" border=1><tr><th>Label</th><th>lang</th></tr>"
            	$(xml).children().each(function(){
            		if(this.tagName=="clslabel" || this.tagName=="proplabel"){
            			output+="<tr><td>"+$(this).attr("value")+"</td><td>"+$(this).attr("lang")+"</td></tr>"
            		}
            	});
    		output+="</table>"
    	}    
        output+="</td>"
        output+="<td align=\"center\"><a target=\"_blank\" href=\""+$(xml).attr("range")+"\">"+((typeof $(xml).attr("range") !== 'undefined')?$(xml).attr("range").substring($(xml).attr("range").lastIndexOf('#')+1):"")+"</a></td>"
		if($(xml).children("valuemapping").length>0 && ($(xml).attr("prop")=="subclass" || $(xml).attr("prop")=="obj")){
        	output+="<td align=center><table width=\"100%\" border=1><tr><th>from</th><th>to</th></tr>"
        	$(xml).children().each(function(){
        		if(this.tagName=="valuemapping"){
        			output+="<tr><td align=center>"+$(this).attr("from")+"</td><td>"
        			if((typeof $(this).attr("propiri") !== 'undefined')){
        				output+="<a href=\""+$(this).attr("propiri")+"\" target=\"_blank\">"+$(this).attr("propiri").substring($(this).attr("propiri").lastIndexOf('#')+1)
        				+"</a>:"
        			}else if((typeof $(xml).attr("propiri") !== 'undefined')){
        				output+="<a href=\""+$(xml).attr("propiri")+"\" target=\"_blank\">"+$(xml).attr("propiri").substring($(xml).attr("propiri").lastIndexOf('#')+1)
        				+"</a>:"
        			}
        			if($(this).attr("to").includes("#")){
        				output+="<a href=\""+$(this).attr("to")+"\" target=\"_blank\">"+$(this).attr("to").substring($(this).attr("to").lastIndexOf('#')+1)
        				+"</a>"
        			}else if($(this).attr("to").includes("/")){
        				output+="<a href=\""+$(this).attr("to")+"\" target=\"_blank\">"+$(this).attr("to").substring($(this).attr("to").lastIndexOf('/')+1)
        				+"</a>"
        			}else{
        				output+="<a href=\""+$(this).attr("to")+"\" target=\"_blank\">"+$(this).attr("to")
        				+"</a>"
        			}	
        			if($(this).children().length>0){
     					output+="<br/>"
        				$(this).children().each(function(){ 
        					if(this.tagName=="addcolumn"){
        						if((typeof $(this).attr("propiri") !== 'undefined')){
        	        				output+="<a href=\""+$(this).attr("propiri")+"\" target=\"_blank\">"+$(this).attr("propiri").substring($(this).attr("propiri").lastIndexOf('#')+1)
        	        				+"</a>:"
        	        			}
        						if((typeof $(this).attr("value") !== 'undefined')){
        						if($(this).attr("value").includes("#")){
        	        				output+="<a href=\""+$(this).attr("value")+"\" target=\"_blank\">"+$(this).attr("value").substring($(this).attr("value").lastIndexOf('#')+1)
        	        				+"</a><br/>"
        	        			}else if($(this).attr("value").includes("/")){
        	        				output+="<a href=\""+$(this).attr("value")+"\" target=\"_blank\">"+$(this).attr("value").substring($(this).attr("value").lastIndexOf('/')+1)
        	        				+"</a><br/>"
        	        			}else{
        	        				output+="<a href=\""+$(this).attr("value")+"\" target=\"_blank\">"+$(this).attr("value")
        	        				+"</a><br/>"
        	        			}
        						}
        					}
        				});
        			}
        			output+="</td></tr>"
        		}
        	});
        	output+="</table></td>"
        }else{
        	output+="<td align=\"center\"><a href=\""+$(xml).attr("concept")+"\" target=\"_blank\" >"+((typeof $(xml).attr("concept") !== 'undefined')?$(xml).attr("concept").substring($(xml).attr("concept").lastIndexOf('/')+1):"")+"</a></td>"
        }
        output+="<td align=\"center\">"       	
        if((typeof $(xml).attr("query") !== 'undefined')){
        	output+=$(xml).attr("query")
        }else if((typeof $(xml).attr("value") !== 'undefined')){
        	if($(xml).attr("value").includes("http")){
        		if($(xml).attr("value").includes("#")){
    				output+="<a href=\""+$(xml).attr("value")+"\" target=\"_blank\">"+$(xml).attr("value").substring($(xml).attr("value").lastIndexOf('#')+1)
    				+"</a><br/>"
    			}else if($(xml).attr("value").includes("/")){
    				output+="<a href=\""+$(xml).attr("value")+"\" target=\"_blank\">"+$(xml).attr("value").substring($(xml).attr("value").lastIndexOf('/')+1)
    				+"</a><br/>"
    			}else{
    				output+="<a href=\""+$(xml).attr("value")+"\" target=\"_blank\">"+$(xml).attr("value")
    				+"</a><br/>"
    			}
        	}else{
            	output+=$(xml).attr("value")
        	}

        }
        output+="</td>"
        output+="<td align=\"center\">"+((typeof $(xml).attr("endpoint") !== 'undefined')?"<a href=\""+$(xml).attr("endpoint")+"\">"+$(xml).attr("endpoint")+"</a>":"")+"</td>"
        if($(xml).attr("splitcharacter") && $(xml).attr("splitposition")){
			output+="<td align=center>^("+$(xml).attr("splitcharacter")+")$</td>"
		}
        else if($(xml).attr("regex")){
			output+="<td align=center>"+$(xml).attr("regex")+"</td>"
		}else{
			output+="<td align=center></td>"
		}
		output+="</tr>";
    }else if(xml.tagName=="columncollection"){
    	console.log("Columncollection!!!")
    	output+="<tr><td align=\"center\" style=\"color:red\">"+((typeof $(xml).attr("name") !== 'undefined')?columnhead+$(xml).attr("name"):"Additional column")+"</td>"
    	output+="<td align=\"center\">obj</td>"
        output+="<td align=\"center\"><a href=\""+$(xml).attr("propiri")+"\" target=\"_blank\" >"+((typeof $(xml).attr("propiri") !== 'undefined')?$(xml).attr("propiri").substring($(xml).attr("propiri").lastIndexOf('/')+1):"")+"</a></td>"
        output+="<td align=center></td>"
        output+="<td align=\"center\"><a href=\""+$(xml).attr("class")+"\" target=\"_blank\" >"+((typeof $(xml).attr("concept") !== 'undefined')?$(xml).attr("concept").substring($(xml).attr("concept").lastIndexOf('/')+1):"")+"</a></td>"
        output+="<tdalign=center></td><td align=center></td></tr>"        
    	columnhead+=$(xml).attr("name")+"<span style=\"color:black\">.</span>"
        $(xml).children().each(function(){
               processColumns(columnhead,this,depth+1)
        });
    }else if(xml.tagName=="classmapping"){
    	classes+="<a target=\"_blank\" href=\""+$(xml).attr("class")+"\">"+$(xml).attr("class")+"</a>&nbsp;"
    }
}



function mappingSchemaReader(url,xml,tableheader,table){
	output=""
    classes=""
    output="<tr><th align=center>Column</th><th align=center>Type</th><th align=center>Property IRI</th><th align=center>Range</th><th align=center>Concept</th><th align=center>Query or Fixed Value</th><th align=center>Endpoint</th><th align=center>Regex</th></tr>"
    console.log(xml)
    classes="<a target=\"_blank\" href=\""+$(xml).find('file').attr("class")+"\">"+$(xml).find('file').attr("class")+"</a>&nbsp;"
    $(xml).find('file').children().each(function(){
            processColumns("",this,1)
    });
    header="Classes: ["+classes+"]<br/>"
    if((typeof $(xml).find('file').attr("indid") !== 'undefined')){
       	header+="Individual ID: "+((typeof $(xml).attr("indidprefix") !== 'undefined')?columnhead+$(xml).attr("indidprefix"):"")+"%%"+$(xml).find('file').attr("indid")+"%%<br/>"
    }else{
       	header+="Individual ID: "+((typeof $(xml).attr("indidprefix") !== 'undefined')?columnhead+$(xml).attr("indidprefix"):"")+"%%GENERATED UUID%%<br/>"
    }
    header+="Namespace: <a target=\"_blank\" href=\""+$(xml).find('file').attr("namespace")+"\">"+$(xml).find('file').attr("namespace")+"</a><br/>"
    header+="Value Namespace: <a target=\"_blank\" href=\""+$(xml).find('file').attr("attnamespace")+"\">"+$(xml).find('file').attr("attnamespace")+"</a><br/>"
    header+="EPSG: <a target=\"_blank\" href=\"http://www.opengis.net/def/crs/EPSG/0/"+$(xml).find('file').attr("epsg")+"\">EPSG:"+$(xml).find('file').attr("epsg")+"</a><br/>"
    output+="<br/><a href=\""+url+"\">Mapping Schema Download</a>"
    $('#'+table).html(output);    	
    $('#'+tableheader).html(header);
}
