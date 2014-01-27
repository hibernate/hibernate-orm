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
import java.util.List;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jaxb.spi.hbm.JaxbReturnPropertyElement;
import org.hibernate.jaxb.spi.hbm.ReturnElement;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.source.LocalBindingContext;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
abstract class AbstractReturnBinder<T extends ReturnElement> {
	protected final MetadataImplementor metadata;
	protected final T element;
	protected final LocalBindingContext context;
	protected final String alias;
	protected final LockMode lockMode;
	protected List<JaxbReturnPropertyElement> returnPropertyElements;

	AbstractReturnBinder(
			final T element, final int elementIndex, final LocalBindingContext context,
			final MetadataImplementor metadata) {
		this.context = context;
		this.metadata = metadata;
		this.element = element;
		this.alias = getAlias( element, elementIndex );
		this.lockMode = Helper.interpretLockMode( element.getLockMode(), context );
		this.returnPropertyElements = element.getReturnProperty();
	}

	private static String getAlias(ReturnElement element, int elementCount) {
		return StringHelper.isEmpty( element.getAlias() ) ? "alias_" + elementCount : element.getAlias();
	}

	abstract NativeSQLQueryReturn process();

	protected void processReturnProperties(ReturnPropertiesCallback callback) {
		for ( JaxbReturnPropertyElement propertyElement : returnPropertyElements ) {
			callback.process( propertyElement );
		}
	}

	static interface ReturnPropertiesCallback {
		void process(JaxbReturnPropertyElement propertyElement);
	}

	static class ReturnPropertiesCallbackImpl implements ReturnPropertiesCallback {
		private final LocalBindingContext bindingContext;
		private final String alias;
		private final Map<String, String[]> propertyResults;

		ReturnPropertiesCallbackImpl(
				final String alias, final Map<String, String[]> propertyResults,
				final LocalBindingContext bindingContext) {
			this.alias = alias;
			this.bindingContext = bindingContext;
			this.propertyResults = propertyResults;
		}

		@Override
		public void process(final JaxbReturnPropertyElement propertyElement) {
			final String name = propertyElement.getName();
			if ( StringHelper.isEmpty( name ) ) {
				throw bindingContext.makeMappingException( "Empty return property name on alias " + alias );
			}
			if ( name.contains( "." ) ) {
				throw bindingContext.makeMappingException(
						"dotted notation in <return-join> or <load_collection> not yet supported"
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
