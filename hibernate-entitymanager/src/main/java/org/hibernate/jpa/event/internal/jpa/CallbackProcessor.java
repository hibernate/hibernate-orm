/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
