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

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.source.FilterSource;
import org.jboss.jandex.AnnotationInstance;

/**
 * @author Steve Ebersole
 */
public class FilterSourceImpl implements FilterSource {
	private final String name;
	private final String condition;
	private final boolean autoAliasInjection;
	private final Map<String, String> aliasTableMap = new HashMap<String, String>();
	private final Map<String, String> aliasEntityMap = new HashMap<String, String>();

	public FilterSourceImpl(AnnotationInstance filterAnnotation, AnnotationBindingContext bindingContext) {
		final ClassLoaderService classLoaderService = bindingContext.getServiceRegistry().getService( ClassLoaderService.class );
		
		this.name = JandexHelper.getValue( filterAnnotation, "name", String.class, classLoaderService );
		this.condition = JandexHelper.getValue( filterAnnotation, "condition", String.class, classLoaderService );
		this.autoAliasInjection = JandexHelper.getValue( filterAnnotation, "deduceAliasInjectionPoints", Boolean.class,
				classLoaderService );

		for ( AnnotationInstance aliasAnnotation : JandexHelper.getValue( filterAnnotation, "aliases",
				AnnotationInstance[].class, classLoaderService ) ) {
			final String alias = JandexHelper.getValue( aliasAnnotation, "alias", String.class, classLoaderService );
			final String table = JandexHelper.getValue( aliasAnnotation, "table", String.class, classLoaderService );
			final String entity = JandexHelper.getValue( aliasAnnotation, "entity", String.class, classLoaderService );
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
