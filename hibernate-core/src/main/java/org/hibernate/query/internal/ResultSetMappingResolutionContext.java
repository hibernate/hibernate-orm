/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Context ("parameter object") used in resolving a {@link NamedResultSetMappingMementoImpl}
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface ResultSetMappingResolutionContext {
	SessionFactoryImplementor getSessionFactory();
}
