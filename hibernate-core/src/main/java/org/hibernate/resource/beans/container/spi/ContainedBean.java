/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.spi;

import org.hibernate.resource.beans.spi.ManagedBean;

/**
 * @author Steve Ebersole
 */
public interface ContainedBean<B> extends ManagedBean<B> {
}
