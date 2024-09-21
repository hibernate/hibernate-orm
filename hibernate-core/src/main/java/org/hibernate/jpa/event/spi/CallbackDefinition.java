/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.event.spi;

import java.io.Serializable;

import org.hibernate.resource.beans.spi.ManagedBeanRegistry;

public interface CallbackDefinition extends Serializable {

	Callback createCallback(ManagedBeanRegistry beanRegistry);

}
