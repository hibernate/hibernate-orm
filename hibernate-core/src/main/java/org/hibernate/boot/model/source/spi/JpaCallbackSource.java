/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
