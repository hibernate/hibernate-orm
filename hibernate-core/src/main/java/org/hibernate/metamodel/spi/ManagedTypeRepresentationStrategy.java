/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.spi;

import org.hibernate.Incubating;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.property.access.spi.PropertyAccess;

/**
 * Defines a singular extension point for capabilities pertaining to
 * a representation mode.  Acts as a factory for delegates encapsulating
 * these capabilities.
 *
 * todo (6.0) : Allow custom ManagedTypeRepresentationStrategy impls and expose ala `@Tuplizer`
 *
 * @apiNote Incubating because ultimately we want to consolidate handling for
 * both IdentifiableType and EmbeddableType types but that requires (planned)
 * changes to the Hibernate type system that will not happen until a later date
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ManagedTypeRepresentationStrategy {
	RepresentationMode getMode();

	/**
	 * Create a delegate capable of instantiating instances of the represented type.
	 */
	<J> Instantiator<J> generateInstantiator();

	/**
	 * Create the property accessor object for the specified attribute
	 */
	PropertyAccess generatePropertyAccess(Property bootAttributeDescriptor);
}
