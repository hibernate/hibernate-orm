/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import org.hibernate.QueryException;
import org.hibernate.Remove;
import org.hibernate.persister.collection.CompositeElementPropertyMapping;
import org.hibernate.type.Type;

/**
 * Contract for all things that know how to map a property to the needed bits of SQL.
 * <p>
 * The column/formula fragments that represent a property in the table defining the property be obtained by
 * calling {@link #toColumns(String)}.
 *
 * <p>
 * Note, the methods here are generally ascribed to accept "property paths".  That is a historical necessity because
 * of how Hibernate originally understood composites (embeddables) internally.  That is in the process of changing
 * as Hibernate has added {@link CompositeElementPropertyMapping}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@Deprecated(since = "6", forRemoval = true)
@Remove
public interface PropertyMapping {

//	/**
//	 * @asciidoc
//	 *
//	 * Resolve a sub-reference relative to this PropertyMapping.  E.g.,
//	 * given the PropertyMapping for an entity named `Person` with an embedded
//	 * property `#name` calling this method with `"name"` returns the
//	 * PropertyMapping for the `Name` embeddable
//	 * <p>
//	 * todo (6.0) : define an exception in the signature for cases where the PropertyMapping
//	 * cannot be de-referenced (basic values)
//	 */
	//	PropertyMapping resolveSubMapping(String name);

	// todo (6.0) : add capability to create SqmPath, i.e.
	// SqmPath createSqmPath(SqmPath<?> lhs, SqmCreationState creationState);

	// todo (6.0) : add capability to resolve SQL tree Expression
	//		actually define this in ter


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// todo (6.0) remove

	/**
	 * Given a component path expression, get the type of the property
	 */
	Type toType(String propertyName) throws QueryException;

	/**
	 * Given a property path, return the corresponding column name(s).
	 */
	String[] toColumns(String propertyName) throws QueryException, UnsupportedOperationException;
}
