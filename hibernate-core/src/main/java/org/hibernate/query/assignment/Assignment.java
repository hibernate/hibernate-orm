/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.assignment;


import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.Incubating;
import org.hibernate.query.restriction.Path;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

/**
 * An assignment to a field or property of an entity or embeddable.
 *
 * @param <T> The target entity type of the assignment
 *
 * @since 7.2
 *
 * @author Gavin King
 */
@Incubating
public interface Assignment<T> {

	/**
	 * An assigment of the given literal value to the given attribute
	 * of the root entity.
	 */
	static <T,X> Assignment<T> set(SingularAttribute<T,X> attribute, X value) {
		return new AttributeAssignment<>( attribute, value );
	}

	/**
	 * An assigment of the given literal value to the entity or embeddable
	 * field or property identified by the given path from the root entity.
	 */
	static <T,X> Assignment<T> set(Path<T,X> path, X value) {
		return new PathAssignment<>( path, value );
	}

	/**
	 * An assigment of the entity or embeddable field or property identified
	 * by the given path from the root entity to the given attribute of the
	 * root entity.
	 */
	static <T,X> Assignment<T> set(SingularAttribute<T,X> attribute, Path<T,X> value) {
		return new PathToAttributeAssignment<>( attribute, value );
	}

	/**
	 * An assigment of one entity or embeddable field or property to another
	 * entity or embeddable field or property, each identified by a given path
	 * from the root entity.
	 */
	static <T,X> Assignment<T> set(Path<T,X> path, Path<T,X> value) {
		return new PathToPathAssignment<>( path, value );
	}

	void apply(SqmUpdateStatement<? extends T> update);
}
