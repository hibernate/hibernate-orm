/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry.selector.spi;

import org.hibernate.boot.registry.selector.internal.LazyServiceResolver;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.JavaServiceLoadable;
import org.hibernate.service.Service;

/**
 * @author Christian Beikov
 */
@JavaServiceLoadable
public interface DialectSelector extends Service, LazyServiceResolver<Dialect> {
}
