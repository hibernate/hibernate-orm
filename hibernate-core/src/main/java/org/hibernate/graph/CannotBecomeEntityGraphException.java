/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph;

import org.hibernate.HibernateException;

/**
 * Indicates an illegal attempt to make a {@link Graph} become
 * an {@link jakarta.persistence.EntityGraph} via {@link Graph#makeRootGraph(String, boolean)}.
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
