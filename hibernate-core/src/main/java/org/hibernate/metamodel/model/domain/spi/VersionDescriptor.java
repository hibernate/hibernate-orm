/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

/**
 * Information about the entity (hierarchy wide) version
 *
 * @author Steve Ebersole
 */
public interface VersionDescriptor<O,J> extends SingularPersistentAttribute<O,J>, BasicValuedNavigable<J> {
	/**
	 * Access to the value that indicates an unsaved (transient) entity
	 *
	 * @return The unsaved value
	 */
	String getUnsavedValue();

	@Override
	default void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitVersion( this );
	}

	VersionSupport getVersionSupport();
}
