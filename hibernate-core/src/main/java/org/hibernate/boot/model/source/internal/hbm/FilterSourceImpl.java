/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
