/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model;

import java.util.List;

import org.hibernate.internal.util.IndexedConsumer;

/**
 * Abstraction for what Hibernate understands about a "class", generally before it has access to
 * the actual {@link Class} reference, if there is a {@code Class} at all (dynamic models).
 *
 * @see ClassDetailsRegistry
 *
 * @author Steve Ebersole
 */
public interface ClassDetails extends AnnotationTarget {
	@Override
	default Kind getKind() {
		return Kind.CLASS;
	}

	/**
	 * The name of the class.
	 * <p/>
	 * Generally this is the same as the {@linkplain #getClassName() class name}.
	 * But in the case of Hibernate's {@code entity-name} feature, this would
	 * be the {@code entity-name}
	 */
	String getName();

	/**
	 * The name of the {@link Class}, or {@code null} for dynamic models.
	 *
	 * @apiNote Will be {@code null} for dynamic models
	 */
	String getClassName();

	/**
	 * Whether the class should be considered abstract.
	 */
	boolean isAbstract();

	/**
	 * Details for the class that is the super type for this class.
	 */
	ClassDetails getSuperType();

	/**
	 * Get the fields for this class
	 */
	List<FieldDetails> getFields();

	/**
	 * Get the methods for this class
	 */
	List<MethodDetails> getMethods();

	/**
	 * Get the member on the class defined as the identifier, if one.  If multiple
	 * members are defined as identifier (non-aggregated), this returns a random one,
	 * although it is verified they all exist on the same "level" - i.e. all on
	 * {@linkplain AnnotationTarget.Kind#FIELD fields} or on
	 * {@linkplain AnnotationTarget.Kind#METHOD methods}.
	 */
	MemberDetails getIdentifierMember();
}
