/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.spi;

/**
 * Common interface for JAXB bindings representing entities and mapped-superclasses.
 */
public interface EntityOrMappedSuperclass extends ManagedType, LifecycleCallbackContainer {

	JaxbIdClass getIdClass();

	void setIdClass(JaxbIdClass value);

	JaxbEmptyType getExcludeDefaultListeners();

	void setExcludeDefaultListeners(JaxbEmptyType value);

	JaxbEmptyType getExcludeSuperclassListeners();

	void setExcludeSuperclassListeners(JaxbEmptyType value);

	JaxbEntityListeners getEntityListeners();

	void setEntityListeners(JaxbEntityListeners value);

	JaxbPrePersist getPrePersist();

	void setPrePersist(JaxbPrePersist value);

	JaxbPostPersist getPostPersist();

	void setPostPersist(JaxbPostPersist value);

	JaxbPreRemove getPreRemove();

	void setPreRemove(JaxbPreRemove value);

	JaxbPostRemove getPostRemove();

	void setPostRemove(JaxbPostRemove value);

	JaxbPreUpdate getPreUpdate();

	void setPreUpdate(JaxbPreUpdate value);

	JaxbPostUpdate getPostUpdate();

	void setPostUpdate(JaxbPostUpdate value);

	JaxbPostLoad getPostLoad();

	void setPostLoad(JaxbPostLoad value);

	JaxbAttributes getAttributes();

	void setAttributes(JaxbAttributes value);

}
