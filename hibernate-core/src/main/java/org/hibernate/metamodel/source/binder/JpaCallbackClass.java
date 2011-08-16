/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.binder;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

public interface JpaCallbackClass {

    /**
     * @param callbackType {@link PrePersist}, {@link PreRemove}, {@link PreUpdate}, {@link PostLoad},
     *        {@link PostPersist}, {@link PostRemove}, or {@link PostUpdate}
     * @return the name of the JPA callback method defined for the associated {@link Entity entity} or {@link MappedSuperclass
     *         mapped superclass} and for the supplied callback annotation class.
     */
    String getCallbackMethod(Class<?> callbackType);

    /**
     * @return the name of the instantiated container where the JPA callbacks for the associated {@link Entity entity} or
     *         {@link MappedSuperclass mapped superclass} are defined. This can be either the entity/mapped superclass itself or an
     *         {@link EntityListeners entity listener}.
     */
    String getName();

    /**
     * @return <code>true</code> if this callback class represents callbacks defined within an {@link EntityListeners entity
     *         listener}.
     */
    boolean isListener();
}
