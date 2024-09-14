/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
 * @author Steve Ebersole
 */
public enum CallbackType {
	PRE_UPDATE( PreUpdate.class ),
	POST_UPDATE( PostUpdate.class ),
	PRE_PERSIST( PrePersist.class ),
	POST_PERSIST( PostPersist.class ),
	PRE_REMOVE( PreRemove.class ),
	POST_REMOVE( PostRemove.class ),
	POST_LOAD( PostLoad.class );

	private final Class<? extends Annotation> callbackAnnotation;

	CallbackType(Class<? extends Annotation> callbackAnnotation) {
		this.callbackAnnotation = callbackAnnotation;
	}

	public Class<? extends Annotation> getCallbackAnnotation() {
		return callbackAnnotation;
	}
}
