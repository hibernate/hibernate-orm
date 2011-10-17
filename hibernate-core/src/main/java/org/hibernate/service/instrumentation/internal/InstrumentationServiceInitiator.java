package org.hibernate.service.instrumentation.internal;

import java.util.Map;

import org.hibernate.service.instrumentation.spi.InstrumentationService;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Simple ServiceInitiator for InstrumentationService
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class InstrumentationServiceInitiator implements BasicServiceInitiator<InstrumentationService> {
	public static final InstrumentationServiceInitiator INSTANCE = new InstrumentationServiceInitiator();

	@Override
	public InstrumentationService initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new CachingInstrumentationService();
	}

	@Override
	public Class<InstrumentationService> getServiceInitiated() {
		return InstrumentationService.class;
	}

}
