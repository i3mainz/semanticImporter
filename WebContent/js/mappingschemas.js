var mappingschemas={
		"POI Berufsfeuerwehr":"schema/bfw.xml",
		"POI Botschaften/Konsulate":"schema/botkon.xml",
		"POI Bundesbehörden":"schema/bbeh.xml",
		"POI Gerichte":"schema/gerichte.xml",
		"POI Hochschulen":"schema/hs.xml",
		"POI Justizvollzugsanstalt":"schema/jva.xml",
		"POI KFZ Zulassungsstelle":"schema/kfz.xml",
		"POI Krankenhäuser":"schema/khv.xml",
		"POI Kita":"schema/kita.xml",
		"POI Landesbehörden":"schema/lbeh.xml",
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
		"Schulen NRW":"schema/nrw_schulen.xml"
}

function mappingSchemaReader(url){
    $.get(url, {}, function (xml){
    	output="<tr><th>Column</th><th>Property IRI</th><th>Range</th><th>Concept</th><th>Options</th></tr>"
    	$(xml).children().each(function(){
            if(this.tagName=="column" || this.tagName=="addcolumn"){
                $output+="<tr><td>"+$(this).attr("name")+"</td><td>"+$(this).attr("propiri")+"</td><td>"+$(this).attr("range")+"</td><td>"+$(this).attr("concept")+"</td><td></td></tr>";
            }
        });
    	$('#datasettable').html(output);
    });
}