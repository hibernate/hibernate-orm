/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

	public CfgXmlAccessServiceImpl(Map<?,?> configurationValues) {
		aggregatedCfgXml = (LoadedConfig) configurationValues.get( LOADED_CONFIG_KEY );
	}

	@Override
	public LoadedConfig getAggregatedConfig() {
		return aggregatedCfgXml;
	}
}
