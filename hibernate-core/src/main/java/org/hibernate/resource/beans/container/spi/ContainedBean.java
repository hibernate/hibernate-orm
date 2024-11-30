/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.spi;

/**
 * @author Steve Ebersole
 */
public interface ContainedBean<B> {
	// todo (5.3) : can we combine ContainedBean and org.hibernate.resource.beans.spi.ManagedBean into the same thing?
	/**
	 * Get the bean instance producer associated with this container-backed bean
	 */
	B getBeanInstance();
}
