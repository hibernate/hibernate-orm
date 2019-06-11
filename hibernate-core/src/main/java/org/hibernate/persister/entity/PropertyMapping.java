/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.QueryException;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.persister.SqlExpressableType;
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
 * todo (6.0) : move to {@link org.hibernate.persister.spi} - that is its more logical home.  AFAIK this
 * 		has never been documented as a public API
 *
 * todo (6.0) : re-word these Javadocs
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface PropertyMapping {

	/**
	 * @asciidoc
	 *
	 * Resolve a sub-reference relative to this PropertyMapping.  E.g.,
	 * given the PropertyMapping for an entity named `Person` with an embedded
	 * property `#name` calling this method with `"name"` returns the
	 * PropertyMapping for the `Name` embeddable
	 * <p>
	 * todo (6.0) : define an exception in the signature for cases where the PropertyMapping
	 * cannot be de-referenced (basic values)
	 */
	default PropertyMapping resolveSubMapping(String name) {
		throw new NotYetImplementedFor6Exception();
	}

	// todo (6.0) : add capability to create SqmPath, i.e.
	// SqmPath createSqmPath(SqmPath<?> lhs, SqmCreationState creationState);

	// todo (6.0) : add capability to resolve SQL tree Expression
	//		actually define this in ter


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// todo (6.0) remove

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
}
