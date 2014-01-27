/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.source.TableSource;
import org.jboss.jandex.AnnotationInstance;

/**
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
class TableSourceImpl implements TableSource {
	private final TableInfo tableInfo;
	private final EntityBindingContext bindingContext;

	TableSourceImpl(AnnotationInstance tableAnnotation, EntityBindingContext bindingContext) {
		this.bindingContext = bindingContext;
		this.tableInfo = createTableInfo( tableAnnotation );
	}

	@Override
	public String getExplicitSchemaName() {
		return tableInfo.getSchema();
	}

	@Override
	public String getExplicitCatalogName() {
		return tableInfo.getCatalog();
	}

	@Override
	public String getExplicitTableName() {
		return tableInfo.getTableName();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		TableSourceImpl that = ( TableSourceImpl ) o;

		if ( tableInfo != null ? !tableInfo.equals( that.tableInfo ) : that.tableInfo != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return tableInfo != null ? tableInfo.hashCode() : 0;
	}

	private TableInfo createTableInfo(AnnotationInstance tableAnnotation) {
		if ( tableAnnotation != null ) {
			return createPrimaryTableInfo( tableAnnotation );
		}
		else {
			return new TableInfo( null, null, null );
		}
	}

	private TableInfo createPrimaryTableInfo(AnnotationInstance tableAnnotation) {
		final String schemaName = determineSchemaName( tableAnnotation );
		final String catalogName = determineCatalogName( tableAnnotation );

		final String explicitTableName = tableAnnotation == null
				? null
				: JandexHelper.getValue( tableAnnotation, "name", String.class,
						bindingContext.getServiceRegistry().getService( ClassLoaderService.class ) );

		return new TableInfo( schemaName, catalogName, explicitTableName );
	}

	private String determineSchemaName(AnnotationInstance tableAnnotation) {
		return tableAnnotation == null
				? null
				: JandexHelper.getValue( tableAnnotation, "schema", String.class,
						bindingContext.getServiceRegistry().getService( ClassLoaderService.class ) );
	}

	private String determineCatalogName(AnnotationInstance tableAnnotation) {
		return tableAnnotation == null
				? null
				: JandexHelper.getValue( tableAnnotation, "catalog", String.class,
						bindingContext.getServiceRegistry().getService( ClassLoaderService.class ) );
	}

	private static class TableInfo {
		private final String schema;
		private final String catalog;
		private final String tableName;

		private TableInfo(String schema, String catalog, String tableName) {
			this.schema = schema;
			this.catalog = catalog;
			this.tableName = tableName;
		}

		public String getSchema() {
			return schema;
		}

		public String getCatalog() {
			return catalog;
		}

		public String getTableName() {
			return tableName;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			TableInfo tableInfo = ( TableInfo ) o;

			if ( catalog != null ? !catalog.equals( tableInfo.catalog ) : tableInfo.catalog != null ) {
				return false;
			}
			if ( schema != null ? !schema.equals( tableInfo.schema ) : tableInfo.schema != null ) {
				return false;
			}
			if ( tableName != null ? !tableName.equals( tableInfo.tableName ) : tableInfo.tableName != null ) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = schema != null ? schema.hashCode() : 0;
			result = 31 * result + ( catalog != null ? catalog.hashCode() : 0 );
			result = 31 * result + ( tableName != null ? tableName.hashCode() : 0 );
			return result;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append( "TableInfo" );
			sb.append( "{schema='" ).append( schema ).append( '\'' );
			sb.append( ", catalog='" ).append( catalog ).append( '\'' );
			sb.append( ", tableName='" ).append( tableName ).append( '\'' );
			sb.append( '}' );
			return sb.toString();
		}
	}
}


