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

function processColumns(columnhead,xml){
	if(this.tagName=="column" || this.tagName=="addcolumn"){
        output+="<tr><td align=\"center\" style=\"color:red\">"+((typeof $(this).attr("name") !== 'undefined')?$(this).attr("name"):"Additional column")+"</td>"
        output+="<td align=\"center\">"+$(this).attr("prop")+"</td>"
        output+="<td align=\"center\"><a href=\""+$(this).attr("propiri")+"\" target=\"_blank\" >"+((typeof $(this).attr("propiri") !== 'undefined')?$(this).attr("propiri").substring($(this).attr("propiri").lastIndexOf('/')+1):"")+"</a></td>"
        output+="<td align=\"center\"><a target=\"_blank\" href=\""+$(this).attr("range")+"\">"+((typeof $(this).attr("range") !== 'undefined')?$(this).attr("range").substring($(this).attr("range").lastIndexOf('#')+1):"")+"</a></td>"
        if($(this).children().length>0 && $(this).attr("prop")=="subclass"){
        	output+="<td><table width=\"100%\" border=1><tr><th>from</th><th>to</th></tr>"
        	$(this).children().each(function(){
        		if(this.tagName=="valuemapping"){
        			output+="<tr><td>"+$(this).attr("from")+"</td><td><a href=\""+$(this).attr("to")+"\" target=\"_blank\">"+$(this).attr("to")+"</a></td></tr>"
        		}
        	});
        	output+="</table></td>"
        }else{
        	output+="<td align=\"center\"><a href=\""+$(this).attr("concept")+"\" target=\"_blank\" >"+((typeof $(this).attr("concept") !== 'undefined')?$(this).attr("concept").substring($(this).attr("concept").lastIndexOf('/')+1):"")+"</a></td>"
        }
        output+="<td align=\"center\">"+((typeof $(this).attr("query") !== 'undefined')?$(this).attr("query"):"")+"</td>"
        output+="<td align=\"center\">"+((typeof $(this).attr("endpoint") !== 'undefined')?"<a href=\""+$(this).attr("endpoint")+"\">"+$(this).attr("endpoint")+"</a>":"")+"</td>"
        output+="</tr>";
    }else if(this.tagName=="columncollection"){
    	
    }
}

function mappingSchemaReader(url){
    $.get(url, {}, function (xml){
    	header="Class: <a target=\"_blank\" href=\""+$(xml).find('file').attr("class")+"\">"+$(xml).find('file').attr("class")+"</a><br/>"
    	header+="Individual ID: "+$(xml).find('file').attr("indid")+"<br/>"
    	header+="Namespace: <a target=\"_blank\" href=\""+$(xml).find('file').attr("namespace")+"\">"+$(xml).find('file').attr("namespace")+"</a><br/>"
    	header+="EPSG: <a target=\"_blank\" href=\"http://www.opengis.net/def/crs/EPSG/0/"+$(xml).find('file').attr("epsg")+"\">EPSG:"+$(xml).find('file').attr("epsg")+"</a><br/>"

    	output="<tr><th>Column</th><th>Type</th><th>Property IRI</th><th>Range</th><th>Concept</th><th>Query</th><th>Endpoint</th></tr>"
    	$(xml).find('file').children().each(function(){
            if(this.tagName=="column" || this.tagName=="addcolumn"){
                output+="<tr><td align=\"center\" style=\"color:red\">"+((typeof $(this).attr("name") !== 'undefined')?$(this).attr("name"):"Additional column")+"</td>"
                output+="<td align=\"center\">"+$(this).attr("prop")+"</td>"
                output+="<td align=\"center\"><a href=\""+$(this).attr("propiri")+"\" target=\"_blank\" >"+((typeof $(this).attr("propiri") !== 'undefined')?$(this).attr("propiri").substring($(this).attr("propiri").lastIndexOf('/')+1):"")+"</a></td>"
                output+="<td align=\"center\"><a target=\"_blank\" href=\""+$(this).attr("range")+"\">"+((typeof $(this).attr("range") !== 'undefined')?$(this).attr("range").substring($(this).attr("range").lastIndexOf('#')+1):"")+"</a></td>"
                if($(this).children().length>0 && $(this).attr("prop")=="subclass"){
                	output+="<td><table width=\"100%\" border=1><tr><th>from</th><th>to</th></tr>"
                	$(this).children().each(function(){
                		if(this.tagName=="valuemapping"){
                			output+="<tr><td>"+$(this).attr("from")+"</td><td><a href=\""+$(this).attr("to")+"\" target=\"_blank\">"+$(this).attr("to")+"</a></td></tr>"
                		}
                	});
                	output+="</table></td>"
                }else{
                	output+="<td align=\"center\"><a href=\""+$(this).attr("concept")+"\" target=\"_blank\" >"+((typeof $(this).attr("concept") !== 'undefined')?$(this).attr("concept").substring($(this).attr("concept").lastIndexOf('/')+1):"")+"</a></td>"
                }
                output+="<td align=\"center\">"+((typeof $(this).attr("query") !== 'undefined')?$(this).attr("query"):"")+"</td>"
                output+="<td align=\"center\">"+((typeof $(this).attr("endpoint") !== 'undefined')?"<a href=\""+$(this).attr("endpoint")+"\">"+$(this).attr("endpoint")+"</a>":"")+"</td>"
                output+="</tr>";
            }else if(this.tagName=="columncollection"){
            	
            }
        });
    	$('#datasettable').html(output);
    	$('#tableheader').html(header);
    });
}
