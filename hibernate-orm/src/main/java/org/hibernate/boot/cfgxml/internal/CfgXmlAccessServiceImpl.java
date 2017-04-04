/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.cfgxml.internal;

import java.util.Map;

import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;

/**
 * @author Steve Ebersole
 */
public class CfgXmlAccessServiceImpl implements CfgXmlAccessService {
	private final LoadedConfig aggregatedCfgXml;

	public CfgXmlAccessServiceImpl(Map configurationValues) {
		aggregatedCfgXml = (LoadedConfig) configurationValues.get( LOADED_CONFIG_KEY );
	}

	@Override
	public LoadedConfig getAggregatedConfig() {
		return aggregatedCfgXml;
	}
}
