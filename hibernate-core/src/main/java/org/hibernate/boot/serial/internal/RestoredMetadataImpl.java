/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial.internal;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.internal.SessionFactoryOptionsCollector;
import org.hibernate.boot.pipeline.internal.ResolvedMappingImplementor;
import org.hibernate.boot.pipeline.internal.SessionFactoryPipeline;
import org.hibernate.boot.serial.RestoredMetadata;

/// Standard [RestoredMetadata] implementation.
///
/// @since 9.0
/// @author Steve Ebersole
public final class RestoredMetadataImpl implements RestoredMetadata {
	private final ResolvedMappingImplementor metadata;

	public RestoredMetadataImpl(ResolvedMappingImplementor metadata) {
		this.metadata = metadata;
	}

	@Override
	public Metadata getMetadata() {
		return metadata;
	}

	@Override
	public SessionFactory buildSessionFactory() {
		return SessionFactoryPipeline.build( metadata, new SessionFactoryOptionsCollector() );
	}
}
