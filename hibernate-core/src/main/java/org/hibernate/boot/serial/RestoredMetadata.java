/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial;

import org.hibernate.Incubating;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;

/// A restored, factory-ready ORM boot model.
///
/// The restored [Metadata] is available for inspection, while
/// [#buildSessionFactory()] provides the supported transition from the archive
/// product to the runtime model.
///
/// @since 9.0
/// @author Steve Ebersole
@Incubating
public interface RestoredMetadata {
	/// The restored boot metadata view.
	///
	/// @return the restored, fully resolved metadata
	Metadata getMetadata();

	/// Builds a SessionFactory from the restored, resolved mapping product.
	///
	/// The caller owns the returned factory and the service registry supplied
	/// when the archive was restored.
	///
	/// @return a new SessionFactory
	SessionFactory buildSessionFactory();
}
