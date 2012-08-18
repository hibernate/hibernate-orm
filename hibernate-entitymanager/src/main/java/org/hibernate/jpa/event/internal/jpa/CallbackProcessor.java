/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.event.internal.jpa;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

/**
 * Delegate for interpreting, parsing and processing callbacks
 *
 * @author Steve Ebersole
 */
public interface CallbackProcessor {
	public static final Class[] CALLBACK_ANNOTATION_CLASSES = new Class[] {
			PreUpdate.class, PostUpdate.class,
			PrePersist.class, PostPersist.class,
			PreRemove.class, PostRemove.class,
			PostLoad.class
	};

	/**
	 * Ugh, Object to account for Configuration/Metamodel split.  Should eventually be EntityBinding from
	 * metamodel code base.  Currently each Integrator method passes in different type and each impl
	 * interprets differently.
	 *
	 * @param entityObject
	 * @param registry
	 */
	public void processCallbacksForEntity(Object entityObject, CallbackRegistryImpl registry);

	public void release();
}
