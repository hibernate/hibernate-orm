/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

/**
 * Non-id, non-version singular attribute
 *
 * @author Steve Ebersole
 */
public interface JaxbLockableAttribute extends JaxbPersistentAttribute {
	boolean isOptimisticLock();
	void setOptimisticLock(Boolean value);
}
