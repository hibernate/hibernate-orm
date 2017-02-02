/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private static Map<String, String> extractConfigParameters(List<JaxbHbmConfigParameterType> paramElementList) {
		if ( CollectionHelper.isEmpty( paramElementList ) ) {
			return Collections.emptyMap();
		}

		final Map<String,String> params = new HashMap<String,String>();
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
