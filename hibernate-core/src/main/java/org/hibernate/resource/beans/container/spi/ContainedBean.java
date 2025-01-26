/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.spi;

import org.hibernate.resource.beans.spi.ManagedBean;

/**
 * @author Steve Ebersole
 */
public interface ContainedBean<B> extends ManagedBean<B> {
}
