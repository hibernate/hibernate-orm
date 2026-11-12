package org.hibernate.context.internal;

import java.util.Map;

import jakarta.annotation.Nonnull;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.context.spi.MultiTenancy;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * The initiator for the {@link MultiTenancy} service.
 *
 * @author Gavin King
 */
public final class MultiTenancyInitiator implements StandardServiceInitiator<MultiTenancy> {
	public static final MultiTenancyInitiator INSTANCE = new MultiTenancyInitiator();

	private MultiTenancyInitiator() {
	}

	@Override
	@Nonnull
	public Class<MultiTenancy> getServiceInitiated() {
		return MultiTenancy.class;
	}

	@Override
	@Nonnull
	public MultiTenancy initiateService(
			@Nonnull Map<String, Object> configurationValues,
			@Nonnull ServiceRegistryImplementor registry) {
		return new MultiTenancy( configurationValues, registry );
	}
}
