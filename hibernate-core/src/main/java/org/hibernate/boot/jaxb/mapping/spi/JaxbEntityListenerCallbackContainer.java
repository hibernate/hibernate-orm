package org.hibernate.boot.jaxb.mapping.spi;

import java.util.List;

/**
 * JAXB binding interface for entity-listener classes, which may declare
 * multiple callback methods for the same lifecycle event, provided they
 * accept different parameter types.
 *
 * @see JaxbLifecycleCallbackContainer
 */
public interface JaxbEntityListenerCallbackContainer {
	List<JaxbLifecycleCallbackImpl> getPreMerge();

	List<JaxbLifecycleCallbackImpl> getPrePersist();

	List<JaxbLifecycleCallbackImpl> getPostPersist();

	List<JaxbLifecycleCallbackImpl> getPreRemove();

	List<JaxbLifecycleCallbackImpl> getPostRemove();

	List<JaxbLifecycleCallbackImpl> getPreUpdate();

	List<JaxbLifecycleCallbackImpl> getPostUpdate();

	List<JaxbLifecycleCallbackImpl> getPreUpsert();

	List<JaxbLifecycleCallbackImpl> getPostUpsert();

	List<JaxbLifecycleCallbackImpl> getPreInsert();

	List<JaxbLifecycleCallbackImpl> getPostInsert();

	List<JaxbLifecycleCallbackImpl> getPreDelete();

	List<JaxbLifecycleCallbackImpl> getPostDelete();

	List<JaxbLifecycleCallbackImpl> getPostLoad();

	List<JaxbLifecycleCallbackImpl> getPostCreate();

	List<JaxbLifecycleCallbackImpl> getPreClose();
}
