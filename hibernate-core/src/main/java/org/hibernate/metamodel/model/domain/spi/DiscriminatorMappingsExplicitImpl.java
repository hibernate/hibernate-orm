/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
public class DiscriminatorMappingsExplicitImpl implements DiscriminatorMappings {
	private final Map<Object, String> discriminatorValueToEntityNameMap;
	private final Map<String, Object> entityNameToDiscriminatorValueMap;

	public DiscriminatorMappingsExplicitImpl(Map<Object, String> discriminatorValueToEntityNameMap) {
		this.discriminatorValueToEntityNameMap = discriminatorValueToEntityNameMap;

		Map<String, Object> entityNameToDiscriminatorValueMap = new HashMap<>();
		for ( Map.Entry<Object, String> entry : discriminatorValueToEntityNameMap.entrySet() ) {
			entityNameToDiscriminatorValueMap.put( entry.getValue(), entry.getKey() );
		}
		this.entityNameToDiscriminatorValueMap = entityNameToDiscriminatorValueMap;
	}

	@Override
	public Object entityNameToDiscriminatorValue(String entityName) {
		return entityNameToDiscriminatorValueMap.get( entityName );
	}

	@Override
	public String discriminatorValueToEntityName(Object discriminatorValue) {
		return discriminatorValueToEntityNameMap.get( discriminatorValue );
	}
}
