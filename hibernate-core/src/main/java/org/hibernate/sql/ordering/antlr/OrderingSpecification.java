/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
