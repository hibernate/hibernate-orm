/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/// Access to a [SessionFactoryImplementor].
///
/// Runtime model objects may hold this access object instead of the concrete
/// SessionFactory when the model object should not care whether the
/// SessionFactory reference was injected later, supplied directly, mocked, or
/// wrapped.  It is not a general model-creation service lookup.  During model
/// creation, callers should prefer the narrow capability accessors on
/// [RuntimeModelCreationContext].
///
/// @since 9.0
/// @author Steve Ebersole
@FunctionalInterface
public interface SessionFactoryAccess {
	/// Obtain the [SessionFactoryImplementor] reference.
	SessionFactoryImplementor getSessionFactory();
}
