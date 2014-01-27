/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.metamodel.internal.source.hbm.parser;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.NamedQueryDefinitionBuilder;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jaxb.spi.hbm.JaxbCacheModeAttribute;
import org.hibernate.jaxb.spi.hbm.JaxbFlushModeAttribute;
import org.hibernate.jaxb.spi.hbm.JaxbQueryElement;
import org.hibernate.jaxb.spi.hbm.JaxbQueryParamElement;
import org.hibernate.jaxb.spi.hbm.JaxbSqlQueryElement;
import org.hibernate.jaxb.spi.hbm.QuerySourceElement;

/**
 * @author Brett Meyer
 */
abstract class AbstractQueryElementContentsParser {
	
	// TODO: Hate the use of QuerySourceElement -- remove/refactor.
	
	public void parse( NamedQueryDefinitionBuilder builder,
			final JaxbQueryElement queryElement ) {
		
		QuerySourceElement element = new QuerySourceElement() {
			@Override
			public List<Serializable> getContent() {
				return queryElement.getContent();
			}

			@Override
			public JaxbCacheModeAttribute getCacheMode() {
				return queryElement.getCacheMode();
			}

			@Override
			public String getCacheRegion() {
				return queryElement.getCacheRegion();
			}

			@Override
			public boolean isCacheable() {
				return queryElement.isCacheable();
			}

			@Override
			public String getComment() {
				return queryElement.getComment();
			}

			@Override
			public Integer getFetchSize() {
				return queryElement.getFetchSize();
			}

			@Override
			public JaxbFlushModeAttribute getFlushMode() {
				return queryElement.getFlushMode();
			}

			@Override
			public String getName() {
				return queryElement.getName();
			}

			@Override
			public boolean isReadOnly() {
				return queryElement.isReadOnly();
			}

			@Override
			public Integer getTimeout() {
				return queryElement.getTimeout();
			}
		};
		
		parse( builder, element );
	}
	
	public void parse( NamedQueryDefinitionBuilder builder,
			final JaxbSqlQueryElement queryElement ) {
		
		QuerySourceElement element = new QuerySourceElement() {
			@Override
			public List<Serializable> getContent() {
				return queryElement.getContent();
			}

			@Override
			public JaxbCacheModeAttribute getCacheMode() {
				return queryElement.getCacheMode();
			}

			@Override
			public String getCacheRegion() {
				return queryElement.getCacheRegion();
			}

			@Override
			public boolean isCacheable() {
				return queryElement.isCacheable();
			}

			@Override
			public String getComment() {
				return queryElement.getComment();
			}

			@Override
			public Integer getFetchSize() {
				return queryElement.getFetchSize();
			}

			@Override
			public JaxbFlushModeAttribute getFlushMode() {
				return queryElement.getFlushMode();
			}

			@Override
			public String getName() {
				return queryElement.getName();
			}

			@Override
			public boolean isReadOnly() {
				return queryElement.isReadOnly();
			}

			@Override
			public Integer getTimeout() {
				return queryElement.getTimeout();
			}
		};
		
		parse( builder, element );
	}

	private void parse( NamedQueryDefinitionBuilder builder,
			QuerySourceElement queryElement ) {
		final String queryName = queryElement.getName();
		final boolean cacheable = queryElement.isCacheable();
		final String region = queryElement.getCacheRegion();
		final Integer timeout = queryElement.getTimeout();
		final Integer fetchSize = queryElement.getFetchSize();
		final boolean readonly = queryElement.isReadOnly();
		final String comment = queryElement.getComment();
		final CacheMode cacheMode = queryElement.getCacheMode() == null
				? null : CacheMode.valueOf( queryElement
						.getCacheMode().value().toUpperCase() );
		final FlushMode flushMode = queryElement.getFlushMode() == null
				? null : FlushMode.valueOf( queryElement
						.getFlushMode().value().toUpperCase() );

		builder.setName( queryName ).setCacheable( cacheable )
				.setCacheRegion( region ).setTimeout( timeout )
				.setFetchSize( fetchSize ).setFlushMode( flushMode )
				.setCacheMode( cacheMode ).setReadOnly( readonly )
				.setComment( comment );

		final List<Serializable> list = queryElement.getContent();
		parse( queryName, list, builder );
	}

	private void parse(String queryName, List<Serializable> contents,
			NamedQueryDefinitionBuilder builder) {
		final Map<String, String> queryParam = new HashMap<String, String>();
		String query = "";
		boolean isQueryDefined = false;
		for ( Serializable obj : contents ) {
			if ( obj == null ) {
				continue;
			}
			else if ( JaxbQueryParamElement.class.isInstance( obj ) ) {
				JaxbQueryParamElement element
						= JaxbQueryParamElement.class.cast( obj );
				queryParam.put( element.getName(), element.getType() );
			}
			else if ( String.class.isInstance( obj ) ) {
				if ( !isQueryDefined ) {
					if ( StringHelper.isNotEmpty( obj.toString().trim() ) ) {
						query = obj.toString().trim();
						isQueryDefined = true;
					}

				}
				else {
					throw new MappingException(
							"Duplicate query string is defined in Named query[+"
									+ queryName + "]" );
				}
			}
			parseExtra( queryName, obj, builder );
		}
		builder.setParameterTypes( queryParam );
		if ( StringHelper.isEmpty( query ) ) {
			throw new MappingException(
					"Named query[" + queryName+ "] has no query string defined");
		}
		builder.setQuery( query );
	}

	protected abstract void parseExtra(String queryName, Serializable obj, NamedQueryDefinitionBuilder builder);
}
