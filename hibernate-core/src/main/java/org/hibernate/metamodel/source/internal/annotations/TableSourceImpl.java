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
package org.hibernate.metamodel.source.internal.annotations;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.spi.TableSource;

import org.jboss.jandex.AnnotationInstance;

/**
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
class TableSourceImpl implements TableSource {
	private final String schema;
	private final String catalog;
	private final String tableName;
	private final String rowId;

	static TableSourceImpl build(AnnotationInstance tableAnnotation, EntityBindingContext bindingContext) {
		// NOTE : ROWID currently not supported outside case of entity primary table
		return build( tableAnnotation, null, bindingContext );
	}

	static TableSourceImpl build(AnnotationInstance tableAnnotation, String rowId, EntityBindingContext bindingContext) {
		if ( tableAnnotation == null ) {
			return new TableSourceImpl( null, null, null, rowId );
		}

		final ClassLoaderService cls = bindingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class );

		String explicitTableName = JandexHelper.getValue(
				tableAnnotation,
				"name",
				String.class,
				cls
		);

		return new TableSourceImpl(
				determineSchemaName( tableAnnotation, cls ),
				determineCatalogName( tableAnnotation, cls ),
				explicitTableName,
				rowId
		);
	}

	private static String determineSchemaName(AnnotationInstance tableAnnotation, ClassLoaderService cls) {
		return JandexHelper.getValue( tableAnnotation, "schema", String.class, cls );
	}

	private static String determineCatalogName(AnnotationInstance tableAnnotation, ClassLoaderService cls) {
		return JandexHelper.getValue( tableAnnotation, "catalog", String.class, cls );
	}

	private TableSourceImpl(String schema, String catalog, String tableName, String rowId) {
		this.schema = schema;
		this.catalog = catalog;
		this.tableName = tableName;
		this.rowId = rowId;
	}

	@Override
	public String getExplicitSchemaName() {
		return schema;
	}

	@Override
	public String getExplicitCatalogName() {
		return catalog;
	}

	@Override
	public String getExplicitTableName() {
		return tableName;
	}

	@Override
	public String getRowId() {
		return rowId;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final TableSourceImpl that = ( TableSourceImpl ) o;
		return EqualsHelper.equals( this.catalog, that.catalog )
				&& EqualsHelper.equals( this.schema, that.schema )
				&& EqualsHelper.equals( this.tableName, that.tableName );
	}

	@Override
	public int hashCode() {
		int result = schema != null ? schema.hashCode() : 0;
		result = 31 * result + ( catalog != null ? catalog.hashCode() : 0 );
		result = 31 * result + ( tableName != null ? tableName.hashCode() : 0 );
		return result;
	}
}


