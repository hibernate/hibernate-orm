/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * JAXB binding interface for commonality between things which
 * allow callback declarations.  This includes <ul>
 *     <li>
 *         entities and mapped-superclasses
 *     </li>
 *     <li>
 *         entity-listener classes
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface JaxbLifecycleCallbackContainer {
	@Nullable JaxbLifecycleCallbackImpl getPreMerge();
	void setPreMerge(@Nullable JaxbLifecycleCallbackImpl value);

	@Nullable JaxbLifecycleCallbackImpl getPrePersist();
	void setPrePersist(@Nullable JaxbLifecycleCallbackImpl value);

	@Nullable JaxbLifecycleCallbackImpl getPostPersist();
	void setPostPersist(@Nullable JaxbLifecycleCallbackImpl value);

	@Nullable JaxbLifecycleCallbackImpl getPreRemove();
	void setPreRemove(@Nullable JaxbLifecycleCallbackImpl value);

	@Nullable JaxbLifecycleCallbackImpl getPostRemove();
	void setPostRemove(@Nullable JaxbLifecycleCallbackImpl value);

	@Nullable JaxbLifecycleCallbackImpl getPreUpdate();
	void setPreUpdate(@Nullable JaxbLifecycleCallbackImpl value);

	@Nullable JaxbLifecycleCallbackImpl getPostUpdate();
	void setPostUpdate(@Nullable JaxbLifecycleCallbackImpl value);

	@Nullable JaxbLifecycleCallbackImpl getPreUpsert();
	void setPreUpsert(@Nullable JaxbLifecycleCallbackImpl value);

	@Nullable JaxbLifecycleCallbackImpl getPostUpsert();
	void setPostUpsert(@Nullable JaxbLifecycleCallbackImpl value);

	@Nullable JaxbLifecycleCallbackImpl getPreInsert();
	void setPreInsert(@Nullable JaxbLifecycleCallbackImpl value);

	@Nullable JaxbLifecycleCallbackImpl getPostInsert();
	void setPostInsert(@Nullable JaxbLifecycleCallbackImpl value);

	@Nullable JaxbLifecycleCallbackImpl getPreDelete();
	void setPreDelete(@Nullable JaxbLifecycleCallbackImpl value);

	@Nullable JaxbLifecycleCallbackImpl getPostDelete();
	void setPostDelete(@Nullable JaxbLifecycleCallbackImpl value);

	@Nullable JaxbLifecycleCallbackImpl getPostLoad();
	void setPostLoad(@Nullable JaxbLifecycleCallbackImpl value);
}
