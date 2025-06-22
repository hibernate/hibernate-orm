/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * JAXB binding interface for commonality between entity and mapped-superclass mappings
 *
 * @author Steve Ebersole
 */
public interface JaxbEntityOrMappedSuperclass extends JaxbManagedType, JaxbLifecycleCallbackContainer {
	@Nullable JaxbIdClassImpl getIdClass();

	void setIdClass(@Nullable JaxbIdClassImpl value);

	@Nullable JaxbEmptyTypeImpl getExcludeDefaultListeners();

	void setExcludeDefaultListeners(@Nullable JaxbEmptyTypeImpl value);

	@Nullable JaxbEmptyTypeImpl getExcludeSuperclassListeners();

	void setExcludeSuperclassListeners(@Nullable JaxbEmptyTypeImpl value);

	@Nullable JaxbEntityListenerContainerImpl getEntityListenerContainer();

	void setEntityListenerContainer(@Nullable JaxbEntityListenerContainerImpl value);
}
