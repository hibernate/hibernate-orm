package org.hibernate.test.schemafilter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.tool.schema.spi.Target;

class RecordingTarget implements Target {
	
	private final Map<String,Pattern> patterns = new HashMap<String, Pattern>();
	private final Map<String,Set<String>> actionsByCategory = new HashMap<String, Set<String>>();
	
	public RecordingTarget() {
		patterns.put( "schema.create", Pattern.compile( "create schema (.*)" ) );
		patterns.put( "schema.drop", Pattern.compile( "drop schema (.*)" ) );
		patterns.put( "table.create", Pattern.compile( "create table (\\S+) .*" ) );
		patterns.put( "table.drop", Pattern.compile( "drop table (.*)" ) );
	}
	
	public Set<String> getActions( String category ) {
		Set<String> result = actionsByCategory.get( category );
		if ( result == null ) {
			result = new HashSet<String>();
			actionsByCategory.put( category, result );
		}
		return result;
	}
	
	@Override
	public void accept( String action ) {
		action = action.toLowerCase();
		
		for ( Entry<String,Pattern> entry : patterns.entrySet() ) {
			String category = entry.getKey();
			Pattern pattern = entry.getValue();
			
			Matcher matcher = pattern.matcher( action );
			if ( matcher.matches() ) {
				getActions( category ).add( matcher.group( 1 ) );
				return;
			}
		}
	}

	@Override
	public boolean acceptsImportScriptActions() {
		return false;
	}

	@Override
	public void prepare() {
		// nothing to do
	}

	@Override
	public void release() {
		// nothing to do
	}
}