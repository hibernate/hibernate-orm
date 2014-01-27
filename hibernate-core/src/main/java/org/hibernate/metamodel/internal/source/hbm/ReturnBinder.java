/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.metamodel.internal.source.hbm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jaxb.spi.hbm.JaxbReturnElement;
import org.hibernate.jaxb.spi.hbm.JaxbReturnPropertyElement;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.source.LocalBindingContext;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
class ReturnBinder extends AbstractReturnBinder<JaxbReturnElement> {
	ReturnBinder(
			final JaxbReturnElement element, final int elementIndex, final LocalBindingContext context,
			final MetadataImplementor metadata) {
		super( element, elementIndex, context, metadata );
	}

	@Override
	NativeSQLQueryReturn process() {
		final String clazz = element.getClazz();
		String entityName = element.getEntityName();
		if ( StringHelper.isEmpty( clazz ) && StringHelper.isEmpty( entityName ) ) {
			throw context.makeMappingException(
					"<return alias='" + alias + "'> must specify either a class or entity-name"
			);
		}
		entityName = StringHelper.isNotEmpty( entityName ) ? entityName : context.qualifyClassName( clazz );
		final EntityBinding entityBinding = metadata.getEntityBinding( entityName );
		// TODO throw exception here??
		//		if ( entityBinding == null ) {
		//			throw bindingContext.makeMappingException( "Can't locate entitybinding" );
		//		}

		final Map<String, String[]> propertyResults = new HashMap<String, String[]>();
		bindDiscriminatorColumn( propertyResults );
		processReturnProperties( new ReturnPropertiesCallbackImpl2( alias, propertyResults, context ) );

		return new NativeSQLQueryRootReturn(
				alias, entityName, propertyResults, lockMode
		);
	}


	private void bindDiscriminatorColumn(final Map<String, String[]> propertyResults) {
		final JaxbReturnElement.JaxbReturnDiscriminator discriminator = element.getReturnDiscriminator();
		if ( discriminator != null && StringHelper.isNotEmpty( discriminator.getColumn() ) ) {
			String discriminatorColumn = StringHelper.unquote( discriminator.getColumn() );
			propertyResults.put( "class", new String[] { discriminatorColumn } );
		}
	}

	private static class ReturnPropertiesCallbackImpl2 implements ReturnPropertiesCallback {
		private final LocalBindingContext bindingContext;
		private final String alias;
		private final Map<String, String[]> propertyResults;

		private ReturnPropertiesCallbackImpl2(
				final String alias, final Map<String, String[]> propertyResults,
				final LocalBindingContext bindingContext) {
			this.alias = alias;
			this.bindingContext = bindingContext;
			this.propertyResults = propertyResults;
		}

		@Override
		public void process(final JaxbReturnPropertyElement propertyElement) {
			final String name = propertyElement.getName();
			if ( "class".equals( name ) ) {
				throw bindingContext.makeMappingException(
						"class is not a valid property name to use in a <return-property>, use <return-discriminator> instead"
				);
			}
			if ( propertyResults.containsKey( name ) ) {
				throw bindingContext.makeMappingException(
						"duplicate return-property for property " + name + " on alias " + alias
				);
			}
			final List<String> returnColumnNames = getResultColumns( propertyElement, bindingContext );

			if ( returnColumnNames.isEmpty() ) {
				throw bindingContext.makeMappingException(
						"return-property for alias " + alias + " must specify at least one column or return-column name"
				);
			}

			propertyResults.put( name, returnColumnNames.toArray( new String[returnColumnNames.size()] ) );
		}

		private static List<String> getResultColumns(
				final JaxbReturnPropertyElement propertyresult, final LocalBindingContext context) {
			List<String> allResultColumns = new ArrayList<String>();
			if ( propertyresult.getColumn() != null ) {
				String column = context.getMetadataImplementor()
						.getObjectNameNormalizer()
						.normalizeIdentifierQuoting( propertyresult.getColumn() );
				allResultColumns.add( column );
			}
			for ( JaxbReturnPropertyElement.JaxbReturnColumn returnColumn : propertyresult.getReturnColumn() ) {
				if ( returnColumn.getName() != null ) {
					String column = context.getMetadataImplementor()
							.getObjectNameNormalizer()
							.normalizeIdentifierQuoting( returnColumn.getName() );

					allResultColumns.add( column );
				}
			}
			return allResultColumns;
		}
	}

}
