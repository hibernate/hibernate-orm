/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa.event.spi;

import java.lang.annotation.Annotation;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

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
	POST_LOAD( PostLoad.class )
	;

	private Class<? extends Annotation> callbackAnnotation;

	CallbackType(Class<? extends Annotation> callbackAnnotation) {
		this.callbackAnnotation = callbackAnnotation;
	}

	public Class<? extends Annotation> getCallbackAnnotation() {
		return callbackAnnotation;
	}
}
