/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

/**
 * Indicates that a reference to an entity did not resolve to
 * a mapped entity class.
 *
 * @apiNote extends {@link IllegalArgumentException} to
 * satisfy a questionable requirement of the JPA criteria
 * query API
 *
 * @since 6.3
 *
 * @author Gavin King
 */
public class EntityReferenceException extends IllegalArgumentException {
	public EntityReferenceException(String message) {
		super(message);
	}
}
