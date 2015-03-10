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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterAliasMappingType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterType;
import org.hibernate.boot.model.source.spi.FilterSource;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class FilterSourceImpl
		extends AbstractHbmSourceNode
		implements FilterSource {
	private final String name;
	private final String condition;
	private final boolean autoAliasInjection;
	private final Map<String, String> aliasTableMap = new HashMap<String, String>();
	private final Map<String, String> aliasEntityMap = new HashMap<String, String>();

	public FilterSourceImpl(
			MappingDocument mappingDocument,
			JaxbHbmFilterType filterElement) {
		super( mappingDocument );
		this.name = filterElement.getName();

		String explicitAutoAliasInjectionSetting = filterElement.getAutoAliasInjection();

		String conditionAttribute = filterElement.getCondition();
		String conditionContent = null;

		for ( Serializable content : filterElement.getContent() ) {
			if ( String.class.isInstance( content ) ) {
				final String str = content.toString();
				if ( !StringHelper.isEmptyOrWhiteSpace( str ) ) {
					conditionContent = str.trim();
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

		this.condition = Helper.coalesce( conditionContent, conditionAttribute );
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
