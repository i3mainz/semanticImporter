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
		getMappingSchema($("#"+identifier).val(),identifier,tableheader,table,false)
       }
    });
}

var editMode=false

function mappingSchemaToTable(identifier,tableheader,table,edit){
	editMode=edit
	getMappingSchema($("#"+identifier).val(),identifier,tableheader,table,edit)
}

function getMappingSchema(schemaname,identifier,tableheader,table,edit){
	  $.ajax({
       url: 'rest/service/getMappingSchema/'+schemaname,
       type: 'GET',         // HTTP-Methode, hier: POST
       processData: false,
       contentType: false,
       success: function(data) { 
       	   mappingSchemaReader('rest/service/getMappingSchema/'+schemaname,data,tableheader,table,edit)
       }
    });
}

var output=""
var classes=""

function processColumns(columnhead,xml,depth,index){
	console.log("Depth "+depth);
	console.log()
	i=1
	if(xml.tagName=="column" || xml.tagName=="addcolumn" || xml.tagName=="classmapping" || xml.tagName=="rootclass"){
        output+="<tr>"
        if((typeof $(xml).attr("name") !== 'undefined')){
        	output+="<td align=\"center\" style=\"color:red\" id=\"datasetcol_"+index+"\">"+columnhead+$(xml).attr("name")+"</td>"
        }else if(xml.tagName=="addcolumn"){
        	output+="<td align=\"center\" style=\"color:green\" id=\"datasetcol_"+index+"\">Additional column</td>"
        }else if(xml.tagName=="classmapping"){
        	output+="<td align=\"center\" style=\"color:green\" id=\"datasetcol_"+index+"\">Additional class</td>"
        }else if(xml.tagName=="rootclass"){
        	output+="<td align=\"center\" style=\"color:green\" id=\"datasetcol_"+index+"\">Root class</td>"
        }
        if((typeof $(xml).attr("prop") !== 'undefined')){
        	output+="<td align=\"center\" id=\"proptypecol_"+index+"\">"+$(xml).attr("prop")+"</td>"
        }else{
        	output+="<td align=\"center\" id=\"proptypecol_"+index+"\">class</td>"
        }
        output+="<td align=\"center\" id=\"coluri_"+index+"\">"
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
        output+="<td align=\"center\"  id=\"colrange_"+index+"\"><a target=\"_blank\" href=\""+$(xml).attr("range")+"\">"+((typeof $(xml).attr("range") !== 'undefined')?$(xml).attr("range").substring($(xml).attr("range").lastIndexOf('#')+1):"")+"</a></td>"
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
        output+="<td align=\"center\" id=\"valuesparql_"+index+"\">"       	
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
        output+="<td align=\"center\" id=\"valueendpoint_"+index+"\">"+((typeof $(xml).attr("endpoint") !== 'undefined')?"<a href=\""+$(xml).attr("endpoint")+"\">"+$(xml).attr("endpoint")+"</a>":"")+"</td>"
        if($(xml).attr("splitcharacter") && $(xml).attr("splitposition")){
			output+="<td align=center id=\"valueregex_"+index+"\">^("+$(xml).attr("splitcharacter")+")$</td>"
		}
        else if($(xml).attr("regex")){
			output+="<td align=center id=\"valueregex_"+index+"\">"+$(xml).attr("regex")+"</td>"
		}else{
			output+="<td align=center id=\"valueregex_"+index+"\"></td>"
		}
		output+="</tr>";
    }else if(xml.tagName=="columncollection"){
    	console.log("Columncollection!!!")
    	output+="<tr><td align=\"center\" style=\"color:red\">"+((typeof $(xml).attr("name") !== 'undefined')?columnhead+$(xml).attr("name"):"Additional column")+"</td>"
    	output+="<td align=\"center\">obj</td>"
        output+="<td align=\"center\"><a href=\""+$(xml).attr("propiri")+"\" target=\"_blank\" >"+((typeof $(xml).attr("propiri") !== 'undefined')?$(xml).attr("propiri").substring($(xml).attr("propiri").lastIndexOf('/')+1):"")+"</a></td>"
        output+="<td align=center></td>"
        output+="<td align=\"center\"><a href=\""+$(xml).attr("class")+"\" target=\"_blank\" >"+((typeof $(xml).attr("concept") !== 'undefined')?$(xml).attr("concept").substring($(xml).attr("concept").lastIndexOf('/')+1):"")+"</a></td>"
        output+="<td align=center></td><td align=center></td></tr>"        
    	columnhead+=$(xml).attr("name")+"<span style=\"color:black\">.</span>"
        $(xml).children().each(function(){
               processColumns(columnhead,this,depth+1)
        });
    }else if(xml.tagName=="classmapping"){
    	classes+="<a target=\"_blank\" href=\""+$(xml).attr("class")+"\">"+$(xml).attr("class")+"</a>&nbsp;"
    }
}

function removeRow(id){
	$('#'+id).remove();
}

function processColumnsEdit(columnhead,xml,depth,index){
	console.log("Depth "+depth);
	console.log()
	if(xml.tagName=="column" || xml.tagName=="addcolumn" || xml.tagName=="classmapping" || xml.tagName=="rootclass"){
        output+="<tr>"
        if((typeof $(xml).attr("name") !== 'undefined')){
        	output+="<td align=\"center\" style=\"color:red\" id=\"datasetcol_"+index+"\">"+columnhead+$(xml).attr("name")+"</td>"
        }else if(xml.tagName=="addcolumn"){
        	output+="<td align=\"center\" style=\"color:green\" id=\"datasetcol_"+index+"\">Additional column</td>"
        }else if(xml.tagName=="classmapping"){
        	output+="<td align=\"center\" style=\"color:green\" id=\"datasetcol_"+index+"\">Additional class</td>"
        }else if(xml.tagName=="rootclass"){
        	output+="<td align=\"center\" style=\"color:green\" id=\"datasetcol_"+index+"\">Root class</td>"
        }
        if((typeof $(xml).attr("prop") !== 'undefined')){
        	output+="<td align=\"center\" id=\"proptypecol_"+index+"\"><select id=\"proptypecol_"+index+"_select\">"
        	if($(xml).attr("prop")=="data"){
        		output+="<option value=\"data\" selected>DatatypeProperty</option>"
        	}else{
        		output+="<option value=\"data\">DatatypeProperty</option>"
        	}
        	if($(xml).attr("prop")=="obj"){
        		output+="<option value=\"obj\" selected>ObjectProperty</option>"
        	}else{
        		output+="<option value=\"obj\">ObjectProperty</option>"
        	}
        	if($(xml).attr("prop")=="annotation"){
        		output+="<option value=\"annotation\" selected>AnnotationProperty</option>"
        	}else{
        		output+="<option value=\"annotation\">AnnotationProperty</option>"
        	}
        	if($(xml).attr("prop")=="subclass"){
        		output+="<option value=\"subclass\" selected>SubClass</option>"
        	}else{
        		output+="<option value=\"subclass\">SubClass</option>"
        	}
        	if($(xml).attr("prop")=="class"){
        		output+="<option value=\"class\" selected>Class</option>"
        	}else{
        		output+="<option value=\"class\">Class</option>"
        	}
        	output+="</select></td>"
        }else{
        	output+="<td align=center id=\"proptypecol_"+index+"\"><select id=\"proptypecol_"+index+"_select\"><option value=\"data\">DatatypeProperty</option><option value=\"obj\">ObjectProperty</option><option value=\"annotation\">AnnotationProperty</option><option value=\"subclass\">SubClass</option><option value=\"class\" selected>Class</option></select></td>"
        }
        output+="<td align=center id=\"coluri_"+index+"\">"
        if((typeof $(xml).attr("propiri") !== 'undefined')){
        	output+="<input type=\"url\" value=\""+$(xml).attr("propiri")+"\"/>"	
        }else if((typeof $(xml).attr("class") !== 'undefined')){
        	output+="<input type=\"url\" value=\""+$(xml).attr("class")+"\"/>"
        }   
	    //output+='<div class="ui-widget">'
	    output+="<br/>TripleStore:<select id=\"triplestoredropdown_property_"+i+"\" onChange=\"setAutoComplete('triplestoredropdown_property_"+i+"','coluri_"+i+"_input','proptypecol_"+i+"_select')\">"
	    output+="<option value=\"aaa7\">AAA7</option>"
	    output+="<option value=\"inspire4\">INSPIRE4</option>"
	    output+="<option value=\"https://www.wikidata.org/w/api.php?action=wbsearchentities&&format=json&language=en&uselang=en&type=property&search=\">Wikidata</option>"
	    output+="<option value=\"xerleben2\">XErleben2</option>"
	    output+="<option value=\"xplanung5\">XPlanung5</option>"
	    output+="</select>"
	    j=0
        if($(xml).children("proplabel").length>0 || $(xml).children("clslabel").length>0){
    		output+="<table width=\"100%\" border=1><tr><th>Label</th><th>lang</th><th>Options</th></tr>"
            	$(xml).children().each(function(){
            		if(this.tagName=="clslabel" || this.tagName=="proplabel"){
            			output+="<tr id=\"proplabel_"+index+"_"+j+"\"><td>"+$(this).attr("value")+"</td><td>"+$(this).attr("lang")+"</td><td><button onclick=\"removeRow('proplabel_"+i+"_"+j+"')\">-</button></tr>"
            			j++;
            		}
            	});
    		output+="</table>"
    	}    
        output+="</td>"
		if (typeof $(xml).attr("range") !== 'undefined'){
			output+="<td align=\"center\" id=\"colrange_"+index+"\"><input type=\"url\" id=\"colrange_\""+index+"\"/ value=\""+$(xml).attr("range")+"\"></td>"
		}else{
			output+="<td align=\"center\" id=\"colrange_"+index+"\"><input type=\"url\" id=\"colrange_\""+index+"\"/ value=\"\"></td>"
		}
		if($(xml).children("valuemapping").length>0 && ($(xml).attr("prop")=="subclass" || $(xml).attr("prop")=="obj")){
        	output+="<td align=center><table width=\"100%\" border=1><tr><th>from</th><th>to</th></tr>"
        	$(xml).children().each(function(){
        		if(this.tagName=="valuemapping"){
        			output+="<tr><td align=center id=\"coluri_"+index+"\">"+$(this).attr("from")+"</td><td>"
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
			if($(xml).attr("concept")){
				output+="<td align=\"center\" id=\"colconcept_"+index+"\"><input id=\"colconcept_"+index+"_val\" type=\"url\" value=\""+$(xml).attr("concept")+"\"/></td>"			
			}else{
				output+="<td align=\"center\" id=\"colconcept_"+index+"\"><input id=\"colconcept_"+index+"_val\" type=\"url\" value=\"\"/></td>"
			}
        }
        output+="<td align=\"center\" id=\"valuesparql_"+index+"\">"       	
        if((typeof $(xml).attr("query") !== 'undefined')){
        	output+="<input type=\"text\" value=\""+$(xml).attr("query")+"\"/>"
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
		if((typeof $(xml).attr("endpoint") !== 'undefined')){
			output+="<td align=\"center\" id=\"valueregex_"+index+"\"><input id=\"valueregex_"+index+"_val\" type=\"url\" value=\""+$(xml).attr("endpoint")+"\"/></td>"		
		}else{
			output+="<td align=\"center\" id=\"valueregex_"+index+"\"><input id=\"valueregex_"+index+"_val\" type=\"url\" value=\"\"/></td>"
		}
        if($(xml).attr("splitcharacter") && $(xml).attr("splitposition")){
			output+="<td align=center id=\"valueregex_"+index+"\">^("+$(xml).attr("splitcharacter")+")$</td>"
		}
        else if($(xml).attr("regex")){
			output+="<td align=center id=\"valueregex_"+index+"\"><input id=\"valueregex_"+index+"_val\" type=\"text\" value=\""+$(xml).attr("regex")+"\"/></td>"
		}else{
			output+="<td align=center id=\"valueregex_"+index+"\"></td>"
		}
		output+="</tr>";
    }else if(xml.tagName=="columncollection"){
    	console.log("Columncollection!!!")
    	output+="<tr><td align=\"center\" id=\"datasetcol_"+index+"\" style=\"color:red\">"+((typeof $(xml).attr("name") !== 'undefined')?columnhead+$(xml).attr("name"):"Additional column")+"</td>"
    	output+="<td align=\"center\" id=\"proptypecol_"+index+"\">obj</td>"
        output+="<td align=\"center\" id=\"coluri_"+index+"\"><a href=\""+$(xml).attr("propiri")+"\" target=\"_blank\" >"+((typeof $(xml).attr("propiri") !== 'undefined')?$(xml).attr("propiri").substring($(xml).attr("propiri").lastIndexOf('/')+1):"")+"</a></td>"
        output+="<td align=center></td>"
        output+="<td align=\"center\"><a href=\""+$(xml).attr("class")+"\" target=\"_blank\" >"+((typeof $(xml).attr("concept") !== 'undefined')?$(xml).attr("concept").substring($(xml).attr("concept").lastIndexOf('/')+1):"")+"</a></td>"
        output+="<td align=center></td><td align=center></td></tr>"        
    	columnhead+=$(xml).attr("name")+"<span style=\"color:black\">.</span>"
        $(xml).children().each(function(){
               processColumnsEdit(columnhead,this,depth+1)
        });
    }else if(xml.tagName=="classmapping"){
    	classes+="<a target=\"_blank\" href=\""+$(xml).attr("class")+"\">"+$(xml).attr("class")+"</a>&nbsp;"
    }
}

function analyzeFile(identifier){
	    var data = new FormData(); // das ist unser Daten-Objekt ...
	    data.append('file', document.getElementById('files').files[0]); // ... an die wir unsere Datei anhngen
	    data.append('fileformat',document.getElementById('fileformat').value)
	   	data.append('serviceurl',document.getElementById('serviceurl').value)
	    $('#loading').show();
	    addcolumncounter=0;
	    $.ajax({
	       url: 'rest/service/analyzeFile', // Wohin soll die Datei geschickt werden?
	       data: data,          // Das ist unser Datenobjekt.
	       type: 'POST',         // HTTP-Methode, hier: POST
	       processData: false,
	       contentType: false,
	       success: function(data) { 
	    	   console.log(data)
	    	    var json=data;
	    	    var table="<tr><td align=center>Dataset Class: <div class=\"ui-widget\"><input id=\"class_'+i+'_input\" name=\""+i+"\"></div></td><td align=center><button id=\"addcolumn\" onclick=\"addColumn('datasettable')\">Add Column</button></td></tr><tr><th align=center>Column</th><th align=center>Type</th><th align=center>Property IRI</th><th align=center>Range</th><th align=center>Concept</th><th align=center>Query or Fixed Value</th><th align=center>Endpoint</th><th align=center>Regex</th></tr>"
	    	    var i=0;
	    	    for(key in json["properties"]){
	    	    	if(json["properties"][i]!="the_geom"){
	    	    	    if(i%2==0){
	    	    	        table+="<tr class=\"even\">"
	    	    	    }else{
	    	    	        table+="<tr class=\"odd\">"
	    	    	    }
	    	    		table+="<td align=center id=\"datasetcol_"+i+"\" style=\"color:red\">"+json["properties"][i]+"</td>"
	    	    		table+="<td align=center id=\"proptypecol_"+i+"\"><select id=\"proptypecol_"+i+"_select\"><option value=\"data\">DatatypeProperty</option><option value=\"obj\">ObjectProperty</option><option value=\"annotation\">AnnotationProperty</option><option value=\"subclass\">SubClass</option></select></td>"
	    	    		table+="<td align=center id=\"coluri_"+i+"\">"
	    	    		table+='<div class="ui-widget"><input id="coluri_'+i+'_input" name='+i+'></div>'//"<input type=\"text\" id=\"coluri_"+i+"_input\" name="+i+">
	    	    		table+="<br/>TripleStore:<select id=\"triplestoredropdown_property_"+i+"\" onChange=\"setAutoComplete('triplestoredropdown_property_"+i+"','coluri_"+i+"_input','proptypecol_"+i+"_select')\">"
	        	    	table+="<option value=\"aaa7\">AAA7</option>"
	    	    		table+="<option value=\"inspire4\">INSPIRE4</option>"
	    	    		table+="<option value=\"https://www.wikidata.org/w/api.php?action=wbsearchentities&&format=json&language=en&uselang=en&type=property&search=\">Wikidata</option>"
	        	    	table+="<option value=\"xerleben2\">XErleben2</option>"
	    	    		table+="<option value=\"xplanung5\">XPlanung5</option>"
	    	    		table+="</select></td>"
	    	    		table+="<td align=center id=\"colrange_"+i+"\"><select id=\"colrange_"+i+"_select\"><option value=\"xsd:double\">xsd:double</option><option value=\"xsd:integer\">xsd:integer</option><option value=\"xsd:string\">xsd:string</option></select></td>"
	    	    		table+="<td align=center><button id=\"\" onClick=\"valueMappingDialog('maptypecol_"+i+"_table')\">Add Value Mapping</button><button id=\"\">Find Concept</button><table border=1 id=\"maptypecol_"+i+"_table\"></table></td>"
	    	    		table+="<td align=center id=\"valuesparql_"+i+"\"><button id=\"colsparql_"+i+"_query\" onClick=\"sparqlDialog('')\">Set SPARQL Query</button><input type=\"text\" id=\"+colsparql_"+i+"_queryinp\"/></td>"
	    	    		table+="<td align=center><select id=\"+colendpoint_"+i+"_select\"><option value=\"http://query.wikidata.org/sparql\">Wikidata</option><option value=\"https://dbpedia.org/sparql\">DBPedia</option></select></td>"
	    	    		table+="<td align=center id=\"valueregex_"+i+"\"><input type=\"text\" id=\"colregex_"+i+"_input\"></td></tr>"
	    	    	}
	    	    	i++;
	    	    }
	    	    $("#"+identifier).html(table)
	    	    console.log(i)
	    	    for(j=1;j<i;j++){
	    	    	console.log('#triplestoredropdown_property_'+j)
	    	    	if($('#triplestoredropdown_property_'+j).val().includes("http")){
		    	    	var url=$('#triplestoredropdown_property_'+j).val()+$("#coluri_"+j+"_input").val()
	    	    		$("#coluri_"+j+"_input").autocomplete({
		    	    	      source: function( request, response ) {
		    	    	        $.ajax( {
		    	    	          url: url,
		    	    	          dataType: "jsonp",
		    	    	          data: {
		    	    	            term: request.term
		    	    	          },
		    	    	          success: function( data ) {
		    	    	        	console.log(data)
		    	    	            response( data );
		    	    	          }
		    	    	        } );
		    	    	      },
		    	    	      minLength: 2,
		    	    	      select: function( event, ui ) {
		    	    	    	$("#coluri_"+j+"_input").val(ui.item.value)
		    	    	      }
		    	    	    } );
	    	    	}else{
	    	    		$("#coluri_"+j+"_input").autocomplete({source: Object.keys(window[$('#triplestoredropdown_property_'+j).val()][$('#proptypecol_'+j+'_select').val()]).sort()} );
	    	    	}
	    	    }
	    	    toggleAnalysis()
	       }
	    });
}

function addColumn(identifier){
	i=$("#"+identifier).find('tbody').children().length
	if(i%2==0){
	    table="<tr class=\"even\">"
	}else{
	    table="<tr class=\"odd\">"
	}
	table+="<td id=\"datasetcol_"+i+"\" style=\"color:green\">Additional column</td>"
	table+="<td id=\"proptypecol_"+i+"\"><select id=\"proptypecol_"+i+"_select\"><option value=\"data\">DatatypeProperty</option><option value=\"obj\">ObjectProperty</option><option value=\"annotation\">AnnotationProperty</option><option value=\"subclass\">SubClass</option></select></td>"
	table+="<td id=\"coluri_"+i+"\">"
	table+='<div class="ui-widget"><input id="coluri_'+i+'_input" name='+i+'></div>'//"<input type=\"text\" id=\"coluri_"+i+"_input\" name="+i+">
	table+="<br/>TripleStore:<select id=\"triplestoredropdown_property_"+i+"\">"
	table+="<option value=\"aaa7\">AAA7</option>"
	table+="<option value=\"inspire4\">INSPIRE4</option>"
	table+="<option value=\"https://www.wikidata.org/w/api.php?action=wbsearchentities&&format=json&language=en&uselang=en&type=property&search=\">Wikidata</option>"
	table+="<option value=\"xerleben2\">XErleben2</option>"
	table+="<option value=\"xplanung5\">XPlanung5</option>"
	table+="</select></td>"
	table+="<td id=\"colrange_"+i+"\"><select><option value=\"xsd:double\">xsd:double</option><option value=\"xsd:integer\">xsd:integer</option><option value=\"xsd:string\">xsd:string</option></select></td>"
	table+="<td><button id=\"\">Add Value Mapping</button><button id=\"\">Find Concept</button></td>"
	table+="<td id=\"valuesparql_"+i+"\"><button id=\"colsparql_"+i+"_query\">Set SPARQL Query</button></td>"
	table+="<td><select><option value=\"http://query.wikidata.org/sparql\">Wikidata</option><option value=\"https://dbpedia.org/sparql\">DBPedia</option></select></td>"
	table+="<td id=\"valueregex_"+i+"\"><input type=\"text\" id=\"colrange_"+i+"_input\"></td></tr>"
	$("#"+identifier).find('tbody').append(table);
}

function mappingSchemaReader(url,xml,tableheader,table,edit){
	output=""
    classes=""
    output="<tr><th align=center>Column</th><th align=center>Type</th><th align=center>Property IRI</th><th align=center>Range</th><th align=center>Concept</th><th align=center>Query or Fixed Value</th><th align=center>Endpoint</th><th align=center>Regex</th></tr>"
    console.log(xml)
    classes="<a target=\"_blank\" href=\""+$(xml).find('file').attr("class")+"\">"+$(xml).find('file').attr("class")+"</a>&nbsp;"
    if(edit){
		var ind=0
    	$(xml).find('file').children().each(function(){
            processColumnsEdit("",this,1,ind)
			ind+=1
    	});
    }else{
		var ind=0
        $(xml).find('file').children().each(function(){
            processColumns("",this,1,ind)
			ind+=1
    	});
    }
    header="Classes: ["+classes+"]<br/>"
    if(edit){
        if((typeof $(xml).find('file').attr("indid") !== 'undefined')){
       		header+="Individual ID: "+((typeof $(xml).attr("indidprefix") !== 'undefined')?columnhead+$(xml).attr("indidprefix"):"")+"%%"+$(xml).find('file').attr("indid")+"%%<br/>"
    	}else{
       		header+="Individual ID: "+((typeof $(xml).attr("indidprefix") !== 'undefined')?columnhead+$(xml).attr("indidprefix"):"")+"%%GENERATED UUID%%<br/>"
    	}
    	header+="Namespace: <input type=\"url\" value=\""+$(xml).find('file').attr("namespace")+"\"/><br/>"
    	header+="Value Namespace: <input type=\"url\" value=\""+$(xml).find('file').attr("attnamespace")+"\"/><br/>"
    	header+="EPSG: <a target=\"_blank\" href=\"http://www.opengis.net/def/crs/EPSG/0/"+$(xml).find('file').attr("epsg")+"\">EPSG:<input type=\"number\" id=\"epsg\" value=\""+$(xml).find('file').attr("epsg")+"\"/><br/>"  
    }else{
        if((typeof $(xml).find('file').attr("indid") !== 'undefined')){
       		header+="Individual ID: "+((typeof $(xml).attr("indidprefix") !== 'undefined')?columnhead+$(xml).attr("indidprefix"):"")+"%%"+$(xml).find('file').attr("indid")+"%%<br/>"
    	}else{
       		header+="Individual ID: "+((typeof $(xml).attr("indidprefix") !== 'undefined')?columnhead+$(xml).attr("indidprefix"):"")+"%%GENERATED UUID%%<br/>"
    	}
   	 	header+="Namespace: <a target=\"_blank\" href=\""+$(xml).find('file').attr("namespace")+"\">"+$(xml).find('file').attr("namespace")+"</a><br/>"
   		header+="Value Namespace: <a target=\"_blank\" href=\""+$(xml).find('file').attr("attnamespace")+"\">"+$(xml).find('file').attr("attnamespace")+"</a><br/>"
    	header+="EPSG: <a target=\"_blank\" href=\"http://www.opengis.net/def/crs/EPSG/0/"+$(xml).find('file').attr("epsg")+"\">EPSG:"+$(xml).find('file').attr("epsg")+"</a><br/>" 
    }
    output+="<br/><a href=\""+url+"\">Mapping Schema Download</a>"
    $('#'+table).html(output);    	
    $('#'+tableheader).html(header);
}
