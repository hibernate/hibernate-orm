/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.boot.jaxb.hbm.spi.ConfigParameterContainer;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmConfigParameterType;
import org.hibernate.internal.util.collections.CollectionHelper;

/**
 * @author Steve Ebersole
 */
public class ConfigParameterHelper {
	public static Map<String, String> extractConfigParameters(ConfigParameterContainer container) {
		return extractConfigParameters( container.getConfigParameters() );
	}

	public static Properties extractConfigParametersAsProperties(ConfigParameterContainer container) {
		final Properties properties = new Properties();
		properties.putAll( extractConfigParameters( container.getConfigParameters() ) );
		return properties;
	}

	private static Map<String, String> extractConfigParameters(List<JaxbHbmConfigParameterType> paramElementList) {
		if ( CollectionHelper.isEmpty( paramElementList ) ) {
			return Collections.emptyMap();
		}

		final Map<String,String> params = new HashMap<>();
		for ( JaxbHbmConfigParameterType paramElement : paramElementList ) {
			params.put(
					paramElement.getName(),
					paramElement.getValue()
			);
		}
		return params;
	}

	private ConfigParameterHelper() {
	}
}
