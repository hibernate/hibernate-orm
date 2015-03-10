/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
