/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemafilter;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.tool.schema.spi.GenerationTarget;

class RecordingTarget implements GenerationTarget {
	public enum Category {
		SCHEMA_CREATE( Pattern.compile( "create schema (.*)" ) ),
		SCHEMA_DROP( Pattern.compile( "drop schema(?: if exists)? (.*)" ) ),
		TABLE_CREATE( Pattern.compile( "create table (\\S+) .*" ) ),
		TABLE_DROP( Pattern.compile( "drop table(?: if exists)? (.*)" ) ),
		SEQUENCE_CREATE(Pattern.compile( "create sequence (.*) start (.*)" )),
		SEQUENCE_DROP(Pattern.compile( "drop sequence if exists (.*)" ));

		private final Pattern pattern;

		Category(Pattern pattern) {
			this.pattern = pattern;
		}

		public Pattern getPattern() {
			return pattern;
		}
	}

	private final EnumMap<Category, Set<String>> actionsByCategory = new EnumMap<Category, Set<String>>( Category.class );

	public Set<String> getActions(Category category) {
		Set<String> result = actionsByCategory.get( category );
		if ( result == null ) {
			result = new HashSet<String>();
			actionsByCategory.put( category, result );
		}
		return result;
	}

	@Override
	public void accept(String action) {
		action = action.toLowerCase();

		for ( Category category : Category.values() ) {
			final Matcher matcher = category.getPattern().matcher( action );
			if ( matcher.matches() ) {
				getActions( category ).add( matcher.group( 1 ) );
				return;
			}
		}
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
