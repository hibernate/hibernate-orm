/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008 Red Hat Inc. or third-party contributors as
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
package org.hibernate.sql.ordering.antlr;

/**
 * Models an ordering specification (<tt>ASCENDING</tt> or <tt>DESCENDING</tt>) within a {@link SortSpecification}.
 *
 * @author Steve Ebersole
 */
public class OrderingSpecification extends NodeSupport {
	public static class Ordering {
		public static final Ordering ASCENDING = new Ordering( "asc" );
		public static final Ordering DESCENDING = new Ordering( "desc" );

		private final String name;

		private Ordering(String name) {
			this.name = name;
		}
	}

	private boolean resolved;
	private Ordering ordering;

	public Ordering getOrdering() {
		if ( !resolved ) {
			ordering = resolve( getText() );
			resolved = true;
		}
		return ordering;
	}

	private static Ordering resolve(String text) {
		if ( Ordering.ASCENDING.name.equals( text ) ) {
			return Ordering.ASCENDING;
		}
		else if ( Ordering.DESCENDING.name.equals( text ) ) {
			return Ordering.DESCENDING;
		}
		else {
			throw new IllegalStateException( "Unknown ordering [" + text + "]" );
		}
	}

	public String getRenderableText() {
		return getOrdering().name;
	}
}
