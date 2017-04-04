/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	public CfgXmlAccessService initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new CfgXmlAccessServiceImpl( configurationValues );
	}

	@Override
	public Class<CfgXmlAccessService> getServiceInitiated() {
		return CfgXmlAccessService.class;
	}
}
