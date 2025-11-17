/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.cfgxml.internal;

import java.util.Map;

import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 */
public class CfgXmlAccessServiceInitiator implements StandardServiceInitiator<CfgXmlAccessService> {
	/**
	 * Singleton access
	 */
	public static final CfgXmlAccessServiceInitiator INSTANCE = new CfgXmlAccessServiceInitiator();

	@Override
	public CfgXmlAccessService initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		return new CfgXmlAccessServiceImpl( configurationValues );
	}

	@Override
	public Class<CfgXmlAccessService> getServiceInitiated() {
		return CfgXmlAccessService.class;
	}
}
