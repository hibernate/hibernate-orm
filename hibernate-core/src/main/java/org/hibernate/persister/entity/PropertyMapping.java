/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;
import org.hibernate.QueryException;
import org.hibernate.type.Type;

/**
 * Contract for all things that know how to map a property to the needed bits of SQL.
 * <p/>
 * The column/formula fragments that represent a property in the table defining the property be obtained by
 * calling either {@link #toColumns(String, String)} or {@link #toColumns(String)} to obtain SQL-aliased
 * column/formula fragments aliased or un-aliased, respectively.
 *
 *
 * <p/>
 * Note, the methods here are generally ascribed to accept "property paths".  That is a historical necessity because
 * of how Hibernate originally understood composites (embeddables) internally.  That is in the process of changing
 * as Hibernate has added {@link org.hibernate.loader.plan.build.internal.spaces.CompositePropertyMapping}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface PropertyMapping {
	/**
	 * Given a component path expression, get the type of the property
	 */
	public Type toType(String propertyName) throws QueryException;

	/**
	 * Obtain aliased column/formula fragments for the specified property path.
	 */
	public String[] toColumns(String alias, String propertyName) throws QueryException;
	/**
	 * Given a property path, return the corresponding column name(s).
	 */
	public String[] toColumns(String propertyName) throws QueryException, UnsupportedOperationException;
	/**
	 * Get the type of the thing containing the properties
	 */
	public Type getType();
}
