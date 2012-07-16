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
package org.hibernate.metamodel.internal.source.hbm;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.internal.jaxb.mapping.hbm.JaxbFilterDefElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbHibernateMapping;
import org.hibernate.metamodel.spi.source.FilterDefinitionSource;
import org.hibernate.metamodel.spi.source.FilterParameterSource;

/**
 * @author Steve Ebersole
 */
public class FilterDefinitionSourceImpl
		extends AbstractHbmSourceNode
		implements FilterDefinitionSource {
	private final String name;
	private final String condition;
	private List<FilterParameterSource> parameterSources;

	public FilterDefinitionSourceImpl(
			MappingDocument mappingDocument,
			JaxbFilterDefElement filterDefElement) {
		super( mappingDocument );
		this.name = filterDefElement.getName();

		String conditionAttribute = filterDefElement.getCondition();
		String conditionContent = null;

		final List<FilterParameterSource> parameterSources = new ArrayList<FilterParameterSource>();
		for ( Object content : filterDefElement.getContent() ) {
			if ( String.class.isInstance( content ) ){
				conditionContent = (String) content;
			}
			else {
				parameterSources.add(
						new FilterParameterSourceImpl( (JaxbFilterDefElement.JaxbFilterParam) content )
				);
			}
		}

		this.condition = Helper.coalesce( conditionContent, conditionAttribute );
		this.parameterSources = parameterSources;
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

	private static class FilterParameterSourceImpl implements FilterParameterSource {
		private final String name;
		private final String type;

		public FilterParameterSourceImpl(JaxbFilterDefElement.JaxbFilterParam filterParamElement) {
			this.name = filterParamElement.getName();
			this.type = filterParamElement.getType();
		}

		@Override
		public String getParameterName() {
			return name;
		}

		@Override
		public String getParameterValueTyeName() {
			return type;
		}
	}
}
