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
package org.hibernate.metamodel.source.internal.annotations;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.spi.FilterDefinitionSource;
import org.hibernate.metamodel.source.spi.FilterParameterSource;

import org.jboss.jandex.AnnotationInstance;

/**
 * @author Steve Ebersole
 */
public class FilterDefinitionSourceImpl implements FilterDefinitionSource {
	private final String name;
	private final String condition;
	private final ClassLoaderService classLoaderService;
	private List<FilterParameterSource> parameterSources;

	public FilterDefinitionSourceImpl(AnnotationInstance filterDefAnnotation, AnnotationBindingContext bindingContext) {
		this.classLoaderService = bindingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class );
		this.name = JandexHelper.getValue( filterDefAnnotation, "name", String.class, classLoaderService );
		this.condition = JandexHelper.getValue( filterDefAnnotation, "defaultCondition", String.class, classLoaderService );
		this.parameterSources = buildParameterSources( filterDefAnnotation );
	}

	private List<FilterParameterSource> buildParameterSources(AnnotationInstance filterDefAnnotation) {
		final List<FilterParameterSource> parameterSources = new ArrayList<FilterParameterSource>();
		for ( AnnotationInstance paramAnnotation : JandexHelper.getValue( filterDefAnnotation, "parameters",
				AnnotationInstance[].class, classLoaderService ) ) {
			parameterSources.add( new FilterParameterSourceImpl( paramAnnotation ) );
		}
		return parameterSources;
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
	public Iterable<FilterParameterSource> getParameterSources() {
		return parameterSources;
	}

	private class FilterParameterSourceImpl implements FilterParameterSource {
		private final String name;
		private final String type;

		public FilterParameterSourceImpl(AnnotationInstance paramAnnotation) {
			this.name = JandexHelper.getValue( paramAnnotation, "name", String.class, classLoaderService );
			this.type = JandexHelper.getValue( paramAnnotation, "type", String.class, classLoaderService );
		}

		@Override
		public String getParameterName() {
			return name;
		}

		@Override
		public String getParameterValueTypeName() {
			return type;
		}
	}
}
