var mappingschemas={
		"NamedPlaces":"schema/geographicalnames.xml",
		"POI Berufsfeuerwehr":"schema/bfw.xml",
		"POI Botschaften/Konsulate":"schema/botkon.xml",
		"POI Bundesbehoerden":"schema/bbeh.xml",
		"POI Gerichte":"schema/gerichte.xml",
		"POI Hochschulen":"schema/hs.xml",
		"POI Justizvollzugsanstalt":"schema/jva.xml",
		"POI KFZ Zulassungsstelle":"schema/kfz.xml",
		"POI Krankenhaeuser":"schema/khv.xml",
		"POI Kita":"schema/kita.xml",
		"POI Landesbehoerden":"schema/lbeh.xml",
		"POI Landespolizei":"schema/lpol.xml",
		"POI Rehaklinik":"schema/rhv.xml",
		"POI Schulen Allgemein":"schema/schulen_allg.xml",
		"POI Schulen Beruf":"schema/schulen_beruf.xml",
		"POI Staatsanwaltschaften":"schema/sta.xml",
		"POI THW":"schema/thw.xml",
		"POI UN Organisationen":"schema/unorg.xml",
		"POI Zoll":"schema/zoll.xml",
		"Schulen Berlin":"schema/berlin_schulen.xml",
		"Schulen Brandenburg":"schema/brandenburg_schulen.xml",
		"Schulen Bremen":"schema/bremen_schulen.xml",
		"Schulen Hamburg":"schema/hamburg_schulen.xml",
		"Schulen Hessen":"schema/hessen_schulen.xml",
		"Schulen NRW":"schema/nrw_schulen.xml",
		"Unesco Heritage Bauhaus":"schema/bauhaus_unesco.xml",
		"Unesco Heritage Hamburg":"schema/hamburg_unesco.xml",
		"Unesco Heritage Quedlinburg":"schema/quedlinburg_unesco.xml",
		"Unesco Heritage Sachsen-Anhalt":"schema/quedlinburg_unesco.xml",
		"XPlanung BP_Plan ST":"schema/xplanung_st_bp_plan.xml",
		"XPlanung FP_Plan ST":"schema/xplanung_st_fp_plan.xml"
}

var output=""

function processColumns(columnhead,xml,depth){
	console.log("Depth "+depth);
	console.log()
	if(xml.tagName=="column" || xml.tagName=="addcolumn"){
        output+="<tr><td align=\"center\" style=\"color:red\">"+((typeof $(xml).attr("name") !== 'undefined')?columnhead+$(xml).attr("name"):"Additional column")+"</td>"
        output+="<td align=\"center\">"+$(xml).attr("prop")+"</td>"
        output+="<td align=\"center\"><a href=\""+$(xml).attr("propiri")+"\" target=\"_blank\" >"+((typeof $(xml).attr("propiri") !== 'undefined')?$(xml).attr("propiri").substring($(xml).attr("propiri").lastIndexOf('/')+1):"")+"</a></td>"
        output+="<td align=\"center\"><a target=\"_blank\" href=\""+$(xml).attr("range")+"\">"+((typeof $(xml).attr("range") !== 'undefined')?$(xml).attr("range").substring($(xml).attr("range").lastIndexOf('#')+1):"")+"</a></td>"
		if($(xml).children().length>0 && $(xml).attr("prop")=="subclass" || $(xml).attr("prop")=="obj"){
        	output+="<td><table width=\"100%\" border=1><tr><th>from</th><th>to</th></tr>"
        	$(xml).children().each(function(){
        		if(this.tagName=="valuemapping"){
        			output+="<tr><td>"+$(this).attr("from")+"</td><td>"
        			if($(this).attr("to").includes("#")){
        				output+="<a href=\""+$(this).attr("to")+"\" target=\"_blank\">"+$(this).attr("to").substring($(this).attr("to").lastIndexOf('#')+1)+"</a></td></tr>"
        			}else if($(this).attr("to").includes("/")){
        				output+="<a href=\""+$(this).attr("to")+"\" target=\"_blank\">"+$(this).attr("to").substring($(this).attr("to").lastIndexOf('/')+1)+"</a></td></tr>"
        			}else{
        				output+="<a href=\""+$(this).attr("to")+"\" target=\"_blank\">"+$(this).attr("to")+"</a></td></tr>"
        			}	
        		}
        	});
        	output+="</table></td>"
        }else{
        	output+="<td align=\"center\"><a href=\""+$(xml).attr("concept")+"\" target=\"_blank\" >"+((typeof $(xml).attr("concept") !== 'undefined')?$(xml).attr("concept").substring($(xml).attr("concept").lastIndexOf('/')+1):"")+"</a></td>"
        }
        output+="<td align=\"center\">"+((typeof $(xml).attr("query") !== 'undefined')?$(this).attr("query"):"")+"</td>"
        output+="<td align=\"center\">"+((typeof $(xml).attr("endpoint") !== 'undefined')?"<a href=\""+$(xml).attr("endpoint")+"\">"+$(xml).attr("endpoint")+"</a>":"")+"</td>"
        if($(xml).attr("splitcharacter") && $(xml).attr("splitposition")){
			output+="<td>^("+$(xml).attr("splitcharacter")+")$</td>"
		}
		if($(xml).attr("regex")){
			output+="<td>"+$(xml).attr("regex")+"</td>"
		}
		output+="</tr>";
    }else if(xml.tagName=="columncollection"){
    	console.log("Columncollection!!!")
    	output+="<tr><td align=\"center\" style=\"color:red\">"+((typeof $(xml).attr("name") !== 'undefined')?columnhead+$(xml).attr("name"):"Additional column")+"</td>"
    	output+="<td align=\"center\">obj</td>"
        output+="<td align=\"center\"><a href=\""+$(xml).attr("propiri")+"\" target=\"_blank\" >"+((typeof $(xml).attr("propiri") !== 'undefined')?$(xml).attr("propiri").substring($(xml).attr("propiri").lastIndexOf('/')+1):"")+"</a></td>"
        output+="<td></td>"
        output+="<td align=\"center\"><a href=\""+$(xml).attr("class")+"\" target=\"_blank\" >"+((typeof $(xml).attr("concept") !== 'undefined')?$(xml).attr("concept").substring($(xml).attr("concept").lastIndexOf('/')+1):"")+"</a></td>"
        output+="<td></td><td></td></tr>"        
    	columnhead+=$(xml).attr("name")+"<span style=\"color:black\">.</span>"
        $(xml).children().each(function(){
               processColumns(columnhead,this,depth+1)
        });
    }
}

function mappingSchemaReader(url){
	output=""
    $.get(url, {}, function (xml){
    	header="Class: <a target=\"_blank\" href=\""+$(xml).find('file').attr("class")+"\">"+$(xml).find('file').attr("class")+"</a><br/>"
    	header+="Individual ID: "+((typeof $(xml).attr("indidprefix") !== 'undefined')?columnhead+$(xml).attr("indidprefix"):"")+""+$(xml).find('file').attr("indid")+"<br/>"
    	header+="Namespace: <a target=\"_blank\" href=\""+$(xml).find('file').attr("namespace")+"\">"+$(xml).find('file').attr("namespace")+"</a><br/>"
    	header+="Value Namespace: <a target=\"_blank\" href=\""+$(xml).find('file').attr("attnamespace")+"\">"+$(xml).find('file').attr("attnamespace")+"</a><br/>"
    	header+="EPSG: <a target=\"_blank\" href=\"http://www.opengis.net/def/crs/EPSG/0/"+$(xml).find('file').attr("epsg")+"\">EPSG:"+$(xml).find('file').attr("epsg")+"</a><br/>"
    	output="<tr><th>Column</th><th>Type</th><th>Property IRI</th><th>Range</th><th>Concept</th><th>Query</th><th>Endpoint</th><th>Regex</th></tr>"
    	$(xml).find('file').children().each(function(){
            processColumns("",this,1)
        });
    	$('#datasettable').html(output);
    	$('#tableheader').html(header);
    });
}
