/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph;

import org.hibernate.HibernateException;

/**
 * Indicates an illegal attempt to make a {@link Graph} become
 * an {@link javax.persistence.EntityGraph} via {@link Graph#makeRootGraph(String, boolean)}.
 * Generally this happens because the Graph describes an embeddable, whereas an EntityGraph
 * by definition is only valid for an entity.
 *
 * @author Steve Ebersole
 */
public class CannotBecomeEntityGraphException extends HibernateException {
	public CannotBecomeEntityGraphException(String message) {
		super( message );
	}
}
