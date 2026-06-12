/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.GlobalRegistrations;

import jakarta.persistence.SharedCacheMode;

/// Immutable binding context derived from the categorized model and bootstrap context.
///
/// The context exposes global source-model registrations and bootstrap services
/// that are stable for the whole binding run.  Per-run mutable state belongs in
/// [BindingStateImpl]; this object is for shared options such as naming
/// strategies, cache mode, and global registrations that later coordinator work
/// will bind into the metadata collector.
///
/// @since 9.0
/// @author Steve Ebersole
public class BindingContextImpl implements BindingContext {
	private final GlobalRegistrations globalRegistrations;

	private final ImplicitNamingStrategy implicitNamingStrategy;
	private final PhysicalNamingStrategy physicalNamingStrategy;
	private final SharedCacheMode sharedCacheMode;
	private final BootstrapContext bootstrapContext;

	public BindingContextImpl(CategorizedDomainModel categorizedDomainModel, BootstrapContext bootstrapContext) {
		this(
				categorizedDomainModel.getGlobalRegistrations(),
				bootstrapContext.getMetadataBuildingOptions().getImplicitNamingStrategy(),
				bootstrapContext.getMetadataBuildingOptions().getPhysicalNamingStrategy(),
				bootstrapContext.getMetadataBuildingOptions().getSharedCacheMode(),
				bootstrapContext
		);
	}

	public BindingContextImpl(
			GlobalRegistrations globalRegistrations,
			ImplicitNamingStrategy implicitNamingStrategy,
			PhysicalNamingStrategy physicalNamingStrategy,
			SharedCacheMode sharedCacheMode,
			BootstrapContext bootstrapContext) {
		this.implicitNamingStrategy = implicitNamingStrategy;
		this.physicalNamingStrategy = physicalNamingStrategy;
		this.bootstrapContext = bootstrapContext;
		this.globalRegistrations = globalRegistrations;
		this.sharedCacheMode = sharedCacheMode;
	}

	@Override
	public BootstrapContext getBootstrapContext() {
		return bootstrapContext;
	}

	@Override
	public GlobalRegistrations getGlobalRegistrations() {
		return globalRegistrations;
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return sharedCacheMode;
	}

	@Override
	public ImplicitNamingStrategy getImplicitNamingStrategy() {
		return implicitNamingStrategy;
	}

	@Override
	public PhysicalNamingStrategy getPhysicalNamingStrategy() {
		return physicalNamingStrategy;
	}
}
