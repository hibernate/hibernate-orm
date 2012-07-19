/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations;

import java.util.HashMap;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.source.FilterSource;

/**
 * @author Steve Ebersole
 */
public class FilterSourceImpl implements FilterSource {
	private final String name;
	private final String condition;
	private final boolean autoAliasInjection;
	private final Map<String, String> aliasTableMap = new HashMap<String, String>();
	private final Map<String, String> aliasEntityMap = new HashMap<String, String>();

	public FilterSourceImpl(AnnotationInstance filterAnnotation) {
		this.name = JandexHelper.getValue( filterAnnotation, "name", String.class );
		this.condition = JandexHelper.getValue( filterAnnotation, "condition", String.class );
		this.autoAliasInjection = JandexHelper.getValue( filterAnnotation, "deduceAliasInjectionPoints", boolean.class );

		for ( AnnotationInstance aliasAnnotation : JandexHelper.getValue( filterAnnotation, "aliases", AnnotationInstance[].class ) ) {
			final String alias = JandexHelper.getValue( aliasAnnotation, "alias", String.class );
			final String table = JandexHelper.getValue( aliasAnnotation, "table", String.class );
			final String entity = JandexHelper.getValue( aliasAnnotation, "entity", String.class );
			if ( StringHelper.isNotEmpty( table ) ) {
				aliasTableMap.put( alias, table );
			}
			else if ( StringHelper.isNotEmpty( entity ) ) {
				aliasEntityMap.put( alias, entity );
			}
			else {
				// todo : throw a mapping exception
			}
		}
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
