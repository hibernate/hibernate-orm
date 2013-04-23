/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.criterion;

import org.hibernate.type.Type;

/**
 * The <tt>criterion</tt> package may be used by applications as a framework for building
 * new kinds of <tt>Projection</tt>. However, it is intended that most applications will
 * simply use the built-in projection types via the static factory methods of this class.
 *
 * The factory methods that take an alias allow the projected value to be referred to by 
 * criterion and order instances.
 *
 * See also the {@link Restrictions} factory methods for generating {@link Criterion} instances
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @see org.hibernate.Criteria
 */
public final class Projections {
	/**
	 * A property value projection
	 *
	 * @param propertyName The name of the property whose values should be projected
	 *
	 * @return The property projection
	 *
	 * @see PropertyProjection
	 */
	public static PropertyProjection property(String propertyName) {
		return new PropertyProjection( propertyName );
	}

	/**
	 * A grouping property value projection
	 *
	 * @param propertyName The name of the property to group
	 *
	 * @return The grouped projection
	 *
	 * @see PropertyProjection
	 */
	public static PropertyProjection groupProperty(String propertyName) {
		return new PropertyProjection( propertyName, true );
	}

	/**
	 * An identifier value projection.
	 *
	 * @return The identifier projection
	 *
	 * @see IdentifierProjection
	 */
	public static IdentifierProjection id() {
		return new IdentifierProjection();
	}

	/**
	 * Create a distinct projection from a projection.
	 *
	 * @param projection The project to treat distinctly
	 *
	 * @return The distinct projection
	 *
	 * @see Distinct
	 */
	public static Projection distinct(Projection projection) {
		return new Distinct( projection );
	}
	
	/**
	 * Create a new projection list.
	 *
	 * @return The projection list
	 */
	public static ProjectionList projectionList() {
		return new ProjectionList();
	}
		
	/**
	 * The query row count, ie. <tt>count(*)</tt>
	 *
	 * @return The projection representing the row count
	 *
	 * @see RowCountProjection
	 */
	public static Projection rowCount() {
		return new RowCountProjection();
	}
	
	/**
	 * A property value count projection
	 *
	 * @param propertyName The name of the property to count over
	 *
	 * @return The count projection
	 *
	 * @see CountProjection
	 */
	public static CountProjection count(String propertyName) {
		return new CountProjection( propertyName );
	}
	
	/**
	 * A distinct property value count projection
	 *
	 * @param propertyName The name of the property to count over
	 *
	 * @return The count projection
	 *
	 * @see CountProjection
	 */
	public static CountProjection countDistinct(String propertyName) {
		return new CountProjection( propertyName ).setDistinct();
	}
	
	/**
	 * A property maximum value projection
	 *
	 * @param propertyName The property for which to find the max
	 *
	 * @return the max projection
	 *
	 * @see AggregateProjection
	 */
	public static AggregateProjection max(String propertyName) {
		return new AggregateProjection( "max", propertyName );
	}
	
	/**
	 * A property minimum value projection
	 *
	 * @param propertyName The property for which to find the min
	 *
	 * @return the min projection
	 *
	 * @see AggregateProjection
	 */
	public static AggregateProjection min(String propertyName) {
		return new AggregateProjection( "min", propertyName );
	}
	
	/**
	 * A property average value projection
	 *
	 * @param propertyName The property over which to find the average
	 *
	 * @return the avg projection
	 *
	 * @see AvgProjection
	 */
	public static AggregateProjection avg(String propertyName) {
		return new AvgProjection( propertyName );
	}
	
	/**
	 * A property value sum projection
	 *
	 * @param propertyName The property over which to sum
	 *
	 * @return the sum projection
	 *
	 * @see AggregateProjection
	 */
	public static AggregateProjection sum(String propertyName) {
		return new AggregateProjection( "sum", propertyName );
	}
	
	/**
	 * Assign an alias to a projection, by wrapping it
	 *
	 * @param projection The projection to be aliased
	 * @param alias The alias to apply
	 *
	 * @return The aliased projection
	 *
	 * @see AliasedProjection
	 */
	public static Projection alias(Projection projection, String alias) {
		return new AliasedProjection( projection, alias );
	}

	/**
	 * A SQL projection, a typed select clause fragment
	 *
	 * @param sql The SQL fragment
	 * @param columnAliases The column aliases
	 * @param types The resulting types
	 *
	 * @return The SQL projection
	 *
	 * @see SQLProjection
	 */
	public static Projection sqlProjection(String sql, String[] columnAliases, Type[] types) {
		return new SQLProjection( sql, columnAliases, types );
	}

	/**
	 * A grouping SQL projection, specifying both select clause and group by clause fragments
	 *
	 * @param sql The SQL SELECT fragment
	 * @param groupBy The SQL GROUP BY fragment
	 * @param columnAliases The column aliases
	 * @param types The resulting types
	 *
	 * @return The SQL projection
	 *
	 * @see SQLProjection
	 */
	@SuppressWarnings("UnusedDeclaration")
	public static Projection sqlGroupProjection(String sql, String groupBy, String[] columnAliases, Type[] types) {
		return new SQLProjection(sql, groupBy, columnAliases, types);
	}

	private Projections() {
		//cannot be instantiated
	}

}
