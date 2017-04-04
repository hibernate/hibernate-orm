/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.walking.spi;

/**
 * Describes aspects of the identifier for an entity
 *
 * @author Steve Ebersole
 */
public interface EntityIdentifierDefinition {
	/**
	 * Is the entity identifier encapsulated?  Meaning, is it represented by a single attribute?
	 *
	 * @return {@code true} indicates the identifier is encapsulated (and therefore this is castable to
	 * {@link EncapsulatedEntityIdentifierDefinition}); {@code false} means it is not encapsulated (and therefore
	 * castable to {@link NonEncapsulatedEntityIdentifierDefinition}).
	 *
	 */
	public boolean isEncapsulated();

	public EntityDefinition getEntityDefinition();
}
