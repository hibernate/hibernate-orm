/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

/**
 * Binding of the discriminator in a entity hierarchy
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public interface DiscriminatorDescriptor<O,J>
		extends DomainTypeExposer<J>, VirtualPersistentAttribute<O,J>, SingularPersistentAttribute<O,J> {
	// todo (6.0) : why does this implement PersistentAttribute?
	//		we do not support a model exposing the discriminator as a real attribute
	// 		`@Discriminator` is only valid on TYPE

	// todo (6.0) : how to handle the `org.hibernate.persister.entity.DiscriminatorMetadata` aspect?

	@Override
	default void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitDiscriminator( this );
	}
}

