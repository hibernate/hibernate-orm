/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.persistence.Index;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class JPAIndexHolder {

	private final String name;
	private final String[] columns;
	private final String[] ordering;
	private final boolean unique;

	public JPAIndexHolder(Index index) {
		StringTokenizer tokenizer = new StringTokenizer( index.columnList(), "," );
		List<String> tmp = new ArrayList<String>();
		while ( tokenizer.hasMoreElements() ) {
			tmp.add( tokenizer.nextToken().trim() );
		}
		this.name = index.name();
		this.columns = new String[tmp.size()];
		this.ordering = new String[tmp.size()];
		this.unique = index.unique();
		initializeColumns( columns, ordering, tmp );
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
			final String tmp = description.toLowerCase();
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
