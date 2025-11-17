/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterAliasMappingType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterType;
import org.hibernate.boot.model.source.spi.FilterSource;
import org.hibernate.internal.util.NullnessHelper;
import org.hibernate.internal.util.StringHelper;

import static org.hibernate.internal.util.StringHelper.isBlank;

/**
 * @author Steve Ebersole
 */
public class FilterSourceImpl
		extends AbstractHbmSourceNode
		implements FilterSource {
	private final String name;
	private final String condition;
	private final boolean autoAliasInjection;
	private final Map<String, String> aliasTableMap = new HashMap<>();
	private final Map<String, String> aliasEntityMap = new HashMap<>();

	public FilterSourceImpl(
			MappingDocument mappingDocument,
			JaxbHbmFilterType filterElement) {
		super( mappingDocument );
		this.name = filterElement.getName();

		String explicitAutoAliasInjectionSetting = filterElement.getAutoAliasInjection();

		String conditionAttribute = filterElement.getCondition();
		String conditionContent = null;

		for ( Serializable content : filterElement.getContent() ) {
			if ( content instanceof String string ) {
				if ( !isBlank( string ) ) {
					conditionContent = string.trim();
				}
			}
			else {
				final JaxbHbmFilterAliasMappingType aliasMapping = JaxbHbmFilterAliasMappingType.class.cast( content );
				if ( StringHelper.isNotEmpty( aliasMapping.getTable() ) ) {
					aliasTableMap.put( aliasMapping.getAlias(), aliasMapping.getTable() );
				}
				else if ( StringHelper.isNotEmpty( aliasMapping.getEntity() ) ) {
					aliasEntityMap.put( aliasMapping.getAlias(), aliasMapping.getTable() );
				}
				else {
					throw new MappingException(
							"filter alias must define either table or entity attribute",
							mappingDocument.getOrigin()
					);
				}
			}
		}

		this.condition = NullnessHelper.coalesce( conditionContent, conditionAttribute );
		this.autoAliasInjection = StringHelper.isNotEmpty( explicitAutoAliasInjectionSetting )
				? Boolean.valueOf( explicitAutoAliasInjectionSetting )
				: true;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getCondition() {
		return condition;
	}

	@Override
	public boolean shouldAutoInjectAliases() {
		return autoAliasInjection;
	}

	@Override
	public Map<String, String> getAliasToTableMap() {
		return aliasTableMap;
	}

	@Override
	public Map<String, String> getAliasToEntityMap() {
		return aliasEntityMap;
	}
}
