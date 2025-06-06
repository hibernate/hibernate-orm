/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

/**
 * Non-id, non-version singular attribute
 *
 * @author Steve Ebersole
 */
public interface JaxbLockableAttribute extends JaxbPersistentAttribute {
	Boolean isOptimisticLock();
	void setOptimisticLock(Boolean value);
}
