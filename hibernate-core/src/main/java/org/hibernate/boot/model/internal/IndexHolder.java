/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import jakarta.persistence.Index;

/**
 * @author Strong Liu
 */
public class IndexHolder {
	private final String name;
	private final String[] columns;
	private final String[] ordering;
	private final boolean unique;

	public IndexHolder(Index index) {
		final StringTokenizer tokenizer = new StringTokenizer( index.columnList(), "," );
		final List<String> parsed = new ArrayList<>();
		while ( tokenizer.hasMoreElements() ) {
			final String trimmed = tokenizer.nextToken().trim();
			if ( !trimmed.isEmpty() ) {
				parsed.add( trimmed ) ;
			}
		}
		this.name = index.name();
		this.columns = new String[parsed.size()];
		this.ordering = new String[parsed.size()];
		this.unique = index.unique();
		initializeColumns( columns, ordering, parsed );
	}

	public String[] getColumns() {
		return columns;
	}

	public String getName() {
		return name;
	}

	public String[] getOrdering() {
		return ordering;
	}

	public boolean isUnique() {
		return unique;
	}

	private void initializeColumns(String[] columns, String[] ordering, List<String> list) {
		for ( int i = 0, size = list.size(); i < size; i++ ) {
			final String description = list.get( i );
			final String tmp = description.toLowerCase(Locale.ROOT);
			if ( tmp.endsWith( " desc" ) ) {
				columns[i] = description.substring( 0, description.length() - 5 );
				ordering[i] = "desc";
			}
			else if ( tmp.endsWith( " asc" ) ) {
				columns[i] = description.substring( 0, description.length() - 4 );
				ordering[i] = "asc";
			}
			else {
				columns[i] = description;
				ordering[i] = null;
			}
		}
	}
}
