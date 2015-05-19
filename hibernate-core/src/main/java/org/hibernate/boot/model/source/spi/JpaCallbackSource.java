/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import java.lang.annotation.Annotation;

/**
 * @author Steve Ebersole
 */
public interface JpaCallbackSource {

	/**
	 * @param callbackType {@link javax.persistence.PrePersist}, {@link javax.persistence.PreRemove}, {@link javax.persistence.PreUpdate}, {@link javax.persistence.PostLoad},
	 *        {@link javax.persistence.PostPersist}, {@link javax.persistence.PostRemove}, or {@link javax.persistence.PostUpdate}
	 * @return the name of the JPA callback method defined for the associated {@link javax.persistence.Entity entity} or {@link javax.persistence.MappedSuperclass
	 *         mapped superclass} and for the supplied callback annotation class.
	 */
	String getCallbackMethod(Class<? extends Annotation> callbackType);

	/**
	 * @return the name of the instantiated container where the JPA callbacks for the associated {@link javax.persistence.Entity entity} or
	 *         {@link javax.persistence.MappedSuperclass mapped superclass} are defined. This can be either the entity/mapped superclass itself or an
	 *         {@link javax.persistence.EntityListeners entity listener}.
	 */
	String getName();

	/**
	 * @return <code>true</code> if this callback class represents callbacks defined within an {@link javax.persistence.EntityListeners entity
	 *         listener}.
	 */
	boolean isListener();
}
