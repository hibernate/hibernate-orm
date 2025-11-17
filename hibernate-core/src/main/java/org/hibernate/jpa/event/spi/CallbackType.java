/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.event.spi;

import java.lang.annotation.Annotation;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/**
 * Enumerates the entity lifecycle callback types defined by JPA.
 *
 * @author Steve Ebersole
 */
public enum CallbackType {
	/**
	 * @see PreUpdate
	 */
	PRE_UPDATE,
	/**
	 * @see PostUpdate
	 */
	POST_UPDATE,
	/**
	 * @see PrePersist
	 */
	PRE_PERSIST,
	/**
	 * @see PostPersist
	 */
	POST_PERSIST,
	/**
	 * @see PreRemove
	 */
	PRE_REMOVE,
	/**
	 * @see PostRemove
	 */
	POST_REMOVE,
	/**
	 * @see PostLoad
	 */
	POST_LOAD;

	/**
	 * The JPA-defined callback annotation type corresponding
	 * to this lifecycle event type.
	 */
	public Class<? extends Annotation> getCallbackAnnotation() {
		return switch ( this ) {
			case PRE_PERSIST -> PrePersist.class;
			case PRE_UPDATE -> PreUpdate.class;
			case PRE_REMOVE -> PreRemove.class;
			case POST_PERSIST -> PostPersist.class;
			case POST_UPDATE -> PostUpdate.class;
			case POST_REMOVE -> PostRemove.class;
			case POST_LOAD -> PostLoad.class;
		};
	}
}
