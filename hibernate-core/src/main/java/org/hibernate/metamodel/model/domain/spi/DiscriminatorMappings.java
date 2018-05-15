/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

/**
 * Contract for mapping discriminator-value to entity-name and
 * entity-name to discriminator-value.
 * <p/>
 * Used from discriminated inheritance as well as Hibernate's ANY mapping
 *
 * @author Steve Ebersole
 */
public interface DiscriminatorMappings {
	/**
	 * Resolve the entity-name for the given discriminator-value.
	 */
	String discriminatorValueToEntityName(Object discriminatorValue);

	/**
	 * Resolve the discriminator-value for the given entity-name.
	 */
	Object entityNameToDiscriminatorValue(String entityName);
}

