package org.hibernate.orm.test.schemafilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;

class RecordingTarget implements GenerationTarget {
	private final Pattern schemaCreatePattern;
	private final Pattern schemaDropPattern;
	private final Pattern tableCreatePattern;
	private final Pattern tableDropPattern;
	private final Pattern sequenceCreatePattern;
	private final Pattern sequenceDropPattern;

	List<String> actions = new ArrayList<>();

	public RecordingTarget(Dialect dialect) {
		schemaCreatePattern = Pattern.compile( "create schema (.*)" );
		schemaDropPattern = Pattern.compile( "drop schema (.*)" );
		tableCreatePattern = Pattern.compile( "create table (\\S+) .*" );
		tableDropPattern = buildTableDropPattern( dialect );
		sequenceCreatePattern = Pattern.compile( "create sequence (.*) start (.*)" );
		sequenceDropPattern = Pattern.compile( "drop sequence if exists (.*)" );
	}

	public Pattern schemaCreateActions() {
		return schemaCreatePattern;
	}

	public Pattern schemaDropActions() {
		return schemaDropPattern;
	}

	public Pattern tableCreateActions() {
		return tableCreatePattern;
	}

	public Pattern tableDropActions() {
		return tableDropPattern;
	}

	public Pattern sequenceCreateActions() {
		return sequenceCreatePattern;
	}

	public Pattern sequenceDropActions() {
		return sequenceDropPattern;
	}

	public Set<String> getActions(Pattern pattern) {
		return actions.stream()
				.map( action -> pattern.matcher( action ) )
				.filter( matcher -> matcher.matches() )
				.map( matcher -> matcher.group( 1 ) )
				.collect( Collectors.toSet() );
	}

	@Override
	public void accept(String action) {
		actions.add( action.toLowerCase() );
	}

	@Override
	public void prepare() {
		// nothing to do
	}

	@Override
	public void release() {
		// nothing to do
	}

	private Pattern buildTableDropPattern(Dialect dialect) {
		StringBuilder builder = new StringBuilder( "drop table " );
		if ( dialect.supportsIfExistsBeforeTableName() ) {
			builder.append( "if exists " );
		}
		builder.append( "(.*)" );
		builder.append( dialect.getCascadeConstraintsString() );
		if ( dialect.supportsIfExistsAfterTableName() ) {
			builder.append( " if exists" );
		}
		return Pattern.compile( builder.toString() );
	}
}