/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm;

/**
 * Indicates that a reference to an entity, that is, a given entity name
 * or Java class object, did not resolve to a known mapped entity type.
 *
 * @apiNote extends {@link IllegalArgumentException} to
 *          satisfy a questionable requirement of the JPA
 *          criteria query API
 *
 * @since 6.3
 *
 * @author Gavin King
 */
public class EntityTypeException extends IllegalArgumentException {
	private final String reference;

	public EntityTypeException(String message, String reference) {
		super(message);
		this.reference = reference;
	}

	/**
	 * The entity name or the name of the Java class.
	 */
	public String getReference() {
		return reference;
	}
}
