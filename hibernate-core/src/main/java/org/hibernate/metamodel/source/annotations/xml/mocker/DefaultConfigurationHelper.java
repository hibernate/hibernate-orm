/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
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
package org.hibernate.metamodel.source.annotations.xml.mocker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.xml.filter.IndexedAnnotationFilter;

/**
 * @author Strong Liu
 */
class DefaultConfigurationHelper {
	static DefaultConfigurationHelper INSTANCE = new DefaultConfigurationHelper();

	private DefaultConfigurationHelper() {
	}

	void apply(Map<DotName, List<AnnotationInstance>> annotationsMap, EntityMappingsMocker.Default defaults) {
		if ( annotationsMap == null || annotationsMap.isEmpty() || defaults == null ) {
			return;
		}
		if ( MockHelper.hasSchemaOrCatalogDefined( defaults ) ) {

			for ( DotName annName : IndexedAnnotationFilter.SCHEMAAWARE_ANNOTATIONS ) {
				if ( annName.equals( JPADotNames.TABLE ) && !annotationsMap.containsKey( JPADotNames.TABLE ) && annotationsMap
						.containsKey( JPADotNames.ENTITY ) ) {
					//if an entity doesn't have a @Table, we create one here
					AnnotationInstance entity = annotationsMap.get( JPADotNames.ENTITY ).get( 0 );
					AnnotationInstance table = AnnotationInstance.create(
							JPADotNames.TABLE, entity.target(), MockHelper.EMPTY_ANNOTATION_VALUE_ARRAY
					);
					List<AnnotationInstance> tableList = new ArrayList<AnnotationInstance>( 1 );
					tableList.add( table );

					annotationsMap.put( JPADotNames.TABLE, tableList );
				}
				if ( annotationsMap.containsKey( annName ) ) {
					overrideScheamCatalogByDefault( annName, annotationsMap, defaults );
				}
			}
		}
		if ( defaults.getCascadePersist() != null && defaults.getCascadePersist() ) {
			for ( DotName annName : IndexedAnnotationFilter.ASSOCIATION_ANNOTATIONS ) {
				if ( annotationsMap.containsKey( annName ) ) {
					addCascadePersistIfNotExist( annName, annotationsMap );
				}
			}
		}
	}

	private void addCascadePersistIfNotExist(DotName annName, Map<DotName, List<AnnotationInstance>> indexedAnnotationMap) {
		List<AnnotationInstance> annotationInstanceList = indexedAnnotationMap.get( annName );
		if ( annotationInstanceList == null || annotationInstanceList.isEmpty() ) {
			return;
		}
		List<AnnotationInstance> newAnnotationInstanceList = new ArrayList<AnnotationInstance>( annotationInstanceList.size() );
		for ( AnnotationInstance annotationInstance : annotationInstanceList ) {
			AnnotationValue cascadeValue = annotationInstance.value( "cascade" );
			List<AnnotationValue> newAnnotationValueList = new ArrayList<AnnotationValue>();
			newAnnotationValueList.addAll( annotationInstance.values() );
			if ( cascadeValue == null ) {
				AnnotationValue temp = AnnotationValue.createEnumValue( "", JPADotNames.CASCADE_TYPE, "PERSIST" );
				cascadeValue = AnnotationValue.createArrayValue( "cascade", new AnnotationValue[] { temp } );
			}
			else {
				newAnnotationValueList.remove( cascadeValue );
				String[] cascadeTypes = cascadeValue.asEnumArray();
				boolean hasPersistDefined = false;
				for ( String type : cascadeTypes ) {
					if ( "PERSIST".equals( type ) ) {
						hasPersistDefined = true;
						break;
					}
				}
				if ( hasPersistDefined ) {
					break;
				}
				String[] newCascadeTypes = new String[cascadeTypes.length + 1];
				newCascadeTypes[0] = "PERSIST";
				System.arraycopy( cascadeTypes, 0, newCascadeTypes, 1, cascadeTypes.length );
				AnnotationValue[] cascades = new AnnotationValue[newCascadeTypes.length];
				for ( int i = 0; i < newCascadeTypes.length; i++ ) {
					cascades[i] = AnnotationValue.createEnumValue( "", JPADotNames.CASCADE_TYPE, newCascadeTypes[i] );
				}
				cascadeValue = AnnotationValue.createArrayValue( "cascade", cascades );

			}
			newAnnotationValueList.add( cascadeValue );

			AnnotationInstance newAnnotationInstance = AnnotationInstance.create(
					annotationInstance.name(),
					annotationInstance.target(),
					MockHelper.toArray( newAnnotationValueList )
			);
			newAnnotationInstanceList.add( newAnnotationInstance );
		}
		indexedAnnotationMap.put( annName, newAnnotationInstanceList );
	}

	//@Table, @CollectionTable, @JoinTable, @SecondaryTable
	private void overrideScheamCatalogByDefault(DotName annName, Map<DotName, List<AnnotationInstance>> indexedAnnotationMap, EntityMappingsMocker.Default defaults) {
		List<AnnotationInstance> annotationInstanceList = indexedAnnotationMap.get( annName );
		if ( annotationInstanceList == null || annotationInstanceList.isEmpty() ) {
			return;
		}
		List<AnnotationInstance> newAnnotationInstanceList = new ArrayList<AnnotationInstance>( annotationInstanceList.size() );
		for ( AnnotationInstance annotationInstance : annotationInstanceList ) {
			if ( annName.equals( IndexedAnnotationFilter.SECONDARY_TABLES ) ) {
				AnnotationInstance[] secondaryTableAnnotationInstanceArray = annotationInstance.value().asNestedArray();
				AnnotationValue[] newAnnotationValueArray = new AnnotationValue[secondaryTableAnnotationInstanceArray.length];
				for ( int i = 0; i < secondaryTableAnnotationInstanceArray.length; i++ ) {
					newAnnotationValueArray[i] = AnnotationValue.createNestedAnnotationValue(
							"", overrideScheamCatalogByDefault(
							secondaryTableAnnotationInstanceArray[i],
							defaults
					)
					);
				}
				AnnotationInstance secondaryTablesAnnotationInstance = AnnotationInstance.create(
						annName,
						annotationInstance.target(),
						new AnnotationValue[] {
								AnnotationValue.createArrayValue( "value", newAnnotationValueArray )
						}
				);
				newAnnotationInstanceList.add( secondaryTablesAnnotationInstance );
			}
			else {
				newAnnotationInstanceList.add( overrideScheamCatalogByDefault( annotationInstance, defaults ) );
			}
		}
		indexedAnnotationMap.put( annName, newAnnotationInstanceList );
	}

	private AnnotationInstance overrideScheamCatalogByDefault(AnnotationInstance annotationInstance, EntityMappingsMocker.Default defaults) {
		List<AnnotationValue> newAnnotationValueList = new ArrayList<AnnotationValue>();
		newAnnotationValueList.addAll( annotationInstance.values() );
		boolean schemaDefined = false;
		boolean catalogDefined = false;
		if ( annotationInstance.value( "schema" ) != null ) {
			schemaDefined = true;
		}
		if ( annotationInstance.value( "catalog" ) != null ) {
			catalogDefined = true;
		}
		if ( schemaDefined && catalogDefined ) {
			return annotationInstance;
		}
		if ( !catalogDefined && StringHelper.isNotEmpty( defaults.getCatalog() ) ) {
			newAnnotationValueList.add(
					AnnotationValue.createStringValue(
							"catalog", defaults.getCatalog()
					)
			);
		}
		if ( !schemaDefined && StringHelper.isNotEmpty( defaults.getSchema() ) ) {
			newAnnotationValueList.add(
					AnnotationValue.createStringValue(
							"schema", defaults.getSchema()
					)
			);
		}
		return AnnotationInstance.create(
				annotationInstance.name(),
				annotationInstance.target(),
				MockHelper.toArray( newAnnotationValueList )
		);

	}
}
