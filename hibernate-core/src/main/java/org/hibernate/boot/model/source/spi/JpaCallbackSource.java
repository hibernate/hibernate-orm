/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import java.lang.annotation.Annotation;

/**
 * @author Steve Ebersole
 */
public interface JpaCallbackSource {

	/**
	 * @param callbackType {@link jakarta.persistence.PrePersist}, {@link jakarta.persistence.PreRemove}, {@link jakarta.persistence.PreUpdate}, {@link jakarta.persistence.PostLoad},
	 *        {@link jakarta.persistence.PostPersist}, {@link jakarta.persistence.PostRemove}, or {@link jakarta.persistence.PostUpdate}
	 * @return the name of the JPA callback method defined for the associated {@linkplain jakarta.persistence.Entity entity} or {@linkplain jakarta.persistence.MappedSuperclass
	 *         mapped superclass} and for the supplied callback annotation class.
	 */
	String getCallbackMethod(Class<? extends Annotation> callbackType);

	/**
	 * @return the name of the instantiated container where the JPA callbacks for the associated {@linkplain jakarta.persistence.Entity entity} or
	 *         {@linkplain jakarta.persistence.MappedSuperclass mapped superclass} are defined. This can be either the entity/mapped superclass itself or an
	 *         {@linkplain jakarta.persistence.EntityListeners entity listener}.
	 */
	String getName();

	/**
	 * @return <code>true</code> if this callback class represents callbacks defined within an {@link jakarta.persistence.EntityListeners entity
	 *         listener}.
	 */
	boolean isListener();
}
