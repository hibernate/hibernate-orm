/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class RecordingTarget implements GenerationTarget {
	private final Pattern schemaNameCreatePattern;
	private final Pattern schemaNameDropPattern;
	private final Pattern tableNameCreatePattern;
	private final Pattern tableCreatePattern;
	private final Pattern tableNameDropPattern;
	private final Pattern sequenceNameCreatePattern;
	private final Pattern sequenceNamrDropPattern;

	List<String> actions = new ArrayList<>();

	public RecordingTarget(Dialect dialect) {
		schemaNameCreatePattern = Pattern.compile( "create schema (.*)" );
		schemaNameDropPattern = Pattern.compile( "drop schema (.*)" );
		tableNameCreatePattern = Pattern.compile( "create table (\\S+) .*" );
		tableCreatePattern = Pattern.compile( "create table (.*)" );
		tableNameDropPattern = buildTableDropPattern( dialect );
		sequenceNameCreatePattern = Pattern.compile( "create sequence (.*) start (.*)" );
		sequenceNamrDropPattern = Pattern.compile( "drop sequence if exists (.*)" );
	}

	public Pattern schemaNameCreateActions() {
		return schemaNameCreatePattern;
	}

	public Pattern schemaNameDropActions() {
		return schemaNameDropPattern;
	}

	public Pattern tableNameCreateActions() {
		return tableNameCreatePattern;
	}

	public Pattern tableCreateActions() {
		return tableCreatePattern;
	}

	public Pattern tableNameDropActions() {
		return tableNameDropPattern;
	}

	public Pattern sequenceNameCreateActions() {
		return sequenceNameCreatePattern;
	}

	public Pattern sequenceNameDropActions() {
		return sequenceNamrDropPattern;
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

	public Set<String> getActions(Pattern pattern) {
		return actions.stream()
				.map( action -> pattern.matcher( action ) )
				.filter( matcher -> matcher.matches() )
				.map( matcher -> matcher.group( 1 ) )
				.collect( Collectors.toSet() );
	}

	public boolean containsAction(Pattern pattern) {
		Set<Boolean> collect = actions.stream()
				.map( action -> pattern.matcher( action ) )
				.filter( matcher -> matcher.matches() )
				.map( matcher -> true )
				.collect( Collectors.toSet() );
		return collect.size() == 1;
	}

	public static BaseMatcher<Set<String>> containsExactly(String... expected) {
		Set<String> exps = new HashSet<>();
		for ( String exp : expected ) {
			exps.add( exp.toLowerCase() );
		}
		return containsExactly( exps );
	}

	private static BaseMatcher<Set<String>> containsExactly(final Set expected) {
		return new BaseMatcher<Set<String>>() {
			@Override
			public boolean matches(Object item) {
				Set set = (Set) item;
				return set.size() == expected.size()
						&& set.containsAll( expected );
			}

			@Override
			public void describeTo(Description description) {
				description.appendText( "Is set containing exactly " + expected );
			}
		};
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

	public void clear() {
		actions.clear();
	}
}
