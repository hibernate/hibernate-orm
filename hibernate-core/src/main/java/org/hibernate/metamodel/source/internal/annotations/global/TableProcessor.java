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
package org.hibernate.metamodel.source.internal.annotations.global;

import java.util.Collection;

import org.hibernate.MappingException;
import org.hibernate.annotations.FetchMode;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.internal.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.internal.annotations.util.AnnotationParserHelper;
import org.hibernate.metamodel.source.internal.annotations.util.EnumConversionHelper;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.InFlightMetadataCollector;
import org.hibernate.metamodel.spi.binding.SecondaryTable;
import org.hibernate.metamodel.spi.relational.ObjectName;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.relational.Table;

import org.jboss.jandex.AnnotationInstance;

/**
 * Binds table related information. This binder is called after the entities are bound.
 *
 * @author Hardy Ferentschik
 */
public class TableProcessor {

	private TableProcessor() {
	}

	/**
	 * Binds {@link org.hibernate.annotations.Tables} and {@link org.hibernate.annotations.Table} annotations to the supplied
	 * metadata.
	 *
	 * @param bindingContext the context for annotation binding
	 */
	public static void bind(AnnotationBindingContext bindingContext) {
		Collection<AnnotationInstance> annotations = bindingContext.getJandexAccess().getIndex().getAnnotations( HibernateDotNames.TABLE );
		for ( AnnotationInstance tableAnnotation : annotations ) {
			bind( bindingContext, tableAnnotation );
		}

		annotations = bindingContext.getJandexAccess().getIndex().getAnnotations( HibernateDotNames.TABLES );
		for ( AnnotationInstance tables : annotations ) {
			for ( AnnotationInstance table : JandexHelper.getValue( tables, "value", AnnotationInstance[].class,
					bindingContext.getServiceRegistry().getService( ClassLoaderService.class ) ) ) {
				bind( bindingContext, table );
			}
		}
	}

	private static void bind(AnnotationBindingContext bindingContext, AnnotationInstance tableAnnotation) {
		InFlightMetadataCollector metadataCollector = bindingContext.getMetadataCollector();
		String tableName = JandexHelper.getValue( tableAnnotation, "appliesTo", String.class,
				bindingContext.getServiceRegistry().getService( ClassLoaderService.class ) );
		ObjectName objectName = ObjectName.parse( tableName );
		Schema schema = metadataCollector.getDatabase().getSchema( objectName.getCatalog(), objectName.getSchema() );
		Table table = schema.locateTable( objectName.getName() );
		if ( table != null ) {
			boolean isSecondaryTable = metadataCollector.getSecondaryTables().containsKey( table.getLogicalName() );
			bindHibernateTableAnnotation( table, tableAnnotation,isSecondaryTable, bindingContext );
		}
		else {
			throw new MappingException( "Can't find table[" + tableName + "] from Annotation @Table" );
		}
	}

	private static void bindHibernateTableAnnotation(
			final Table table,
			final AnnotationInstance tableAnnotation,
			final boolean isSecondaryTable,
			final AnnotationBindingContext bindingContext) {
		final ClassLoaderService classLoaderService = bindingContext.getServiceRegistry().getService( ClassLoaderService.class );
		String comment = JandexHelper.getValue( tableAnnotation, "comment", String.class,
				classLoaderService );
		if ( StringHelper.isNotEmpty( comment ) ) {
			table.addComment( comment.trim() );
		}
		if ( !isSecondaryTable ) {
			return;
		}
		SecondaryTable secondaryTable = bindingContext.getMetadataCollector().getSecondaryTables().get( table.getLogicalName() );
		if ( tableAnnotation.value( "fetch" ) != null ) {
			FetchMode fetchMode = JandexHelper.getEnumValue( tableAnnotation, "fetch", FetchMode.class,
					classLoaderService );
			secondaryTable.setFetchStyle( EnumConversionHelper.annotationFetchModeToFetchStyle( fetchMode ) );
		}
		if ( tableAnnotation.value( "inverse" ) != null ) {
			secondaryTable.setInverse( tableAnnotation.value( "inverse" ).asBoolean() );
		}
		if ( tableAnnotation.value( "optional" ) != null ) {
			secondaryTable.setOptional( tableAnnotation.value( "optional" ).asBoolean() );
		}

		if ( tableAnnotation.value( "sqlInsert" ) != null ) {
			secondaryTable.setCustomInsert(
					AnnotationParserHelper.createCustomSQL(
							tableAnnotation.value( "sqlInsert" )
									.asNested()
					)
			);
		}
		if ( tableAnnotation.value( "sqlUpdate" ) != null ) {
			secondaryTable.setCustomUpdate(
					AnnotationParserHelper.createCustomSQL(
							tableAnnotation.value( "sqlUpdate" )
									.asNested()
					)
			);

		}
		if ( tableAnnotation.value( "sqlDelete" ) != null ) {
			secondaryTable.setCustomDelete(
					AnnotationParserHelper.createCustomSQL(
							tableAnnotation.value( "sqlDelete" )
									.asNested()
					)
			);
		}
		// TODO: ForeignKey is not binded right now, because constrint name is not modifyable after it is set
		// another option would be create something like tableDefinition and look up it when we bind table / secondary table

//		if ( tableAnnotation.value( "foreignKey" ) != null ) {
//			AnnotationInstance foreignKeyAnnotation = tableAnnotation.value( "foreignKey" ).asNested();
//			if ( foreignKeyAnnotation.value( "name" ) != null ) {
//				secondaryTable.getForeignKeyReference().setName( foreignKeyAnnotation.value( "name" ).asString() );
//			}
//		}


	}
}
