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
import org.jboss.logging.Logger;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.jaxb.mapping.orm.JaxbEmbeddable;
import org.hibernate.internal.jaxb.mapping.orm.JaxbEntity;
import org.hibernate.internal.jaxb.mapping.orm.JaxbMappedSuperclass;
import org.hibernate.internal.jaxb.mapping.orm.JaxbTable;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;
import org.hibernate.metamodel.source.annotations.xml.filter.IndexedAnnotationFilter;

/**
 * @author Strong Liu
 */
class DefaultConfigurationHelper {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			DefaultConfigurationHelper.class.getName()
	);
	static final DefaultConfigurationHelper INSTANCE = new DefaultConfigurationHelper();
	static final DotName[] GLOBAL_ANNOTATIONS = new DotName[] {
			JPADotNames.SEQUENCE_GENERATOR,
			JPADotNames.TABLE_GENERATOR,
			JPADotNames.NAMED_QUERIES,
			JPADotNames.NAMED_QUERY,
			JPADotNames.NAMED_NATIVE_QUERIES,
			JPADotNames.NAMED_NATIVE_QUERY,
			JPADotNames.SQL_RESULT_SET_MAPPING,
			JPADotNames.SQL_RESULT_SET_MAPPINGS
	};
	static final DotName[] SCHEMA_AWARE_ANNOTATIONS = new DotName[] {
			JPADotNames.TABLE,
			JPADotNames.JOIN_TABLE,
			JPADotNames.COLLECTION_TABLE,
			JPADotNames.SECONDARY_TABLE,
			JPADotNames.SECONDARY_TABLES,
			JPADotNames.TABLE_GENERATOR,
			JPADotNames.SEQUENCE_GENERATOR
	};
	static final DotName[] ASSOCIATION_ANNOTATIONS = new DotName[] {
			JPADotNames.ONE_TO_MANY, JPADotNames.ONE_TO_ONE, JPADotNames.MANY_TO_ONE, JPADotNames.MANY_TO_MANY
	};

	private DefaultConfigurationHelper() {
	}

	void applyDefaults(SchemaAware schemaAware, EntityMappingsMocker.Default defaults) {
		if ( hasSchemaOrCatalogDefined( defaults ) ) {
			if ( StringHelper.isEmpty( schemaAware.getSchema() ) ) {
				schemaAware.setSchema( defaults.getSchema() );
			}
			if ( StringHelper.isEmpty( schemaAware.getCatalog() ) ) {
				schemaAware.setCatalog( defaults.getCatalog() );
			}
		}
	}

	void applyDefaults(Map<DotName, List<AnnotationInstance>> annotationsMap, EntityMappingsMocker.Default defaults) {
		if ( annotationsMap.isEmpty() || defaults == null ) {
			return;
		}
		if ( hasSchemaOrCatalogDefined( defaults ) ) {
			applyDefaultSchemaAndCatalog( annotationsMap, defaults );
		}
		if ( defaults.isCascadePersist()!=null && defaults.isCascadePersist() ) {
			applyDefaultCascadePersist( annotationsMap );
		}
	}

	void applyDefaults(JaxbMappedSuperclass mappedSuperclass, EntityMappingsMocker.Default defaults) {
		applyDefaultsToEntityObject( new MappedSuperClassEntityObject( mappedSuperclass ), defaults );
	}

	void applyDefaults(JaxbEmbeddable embeddable, EntityMappingsMocker.Default defaults) {
		applyDefaultsToEntityObject( new EmbeddableEntityObject( embeddable ), defaults );
	}

	void applyDefaults(JaxbEntity entity, EntityMappingsMocker.Default defaults) {
		mockTableIfNonExist( entity, defaults );
		applyDefaultsToEntityObject( new EntityEntityObject( entity ), defaults );
	}

	private void applyDefaultsToEntityObject(EntityObject entityObject, EntityMappingsMocker.Default defaults) {
		if ( defaults == null ) {
			return;
		}
		String className = MockHelper.buildSafeClassName( entityObject.getClazz(), defaults.getPackageName() );
		entityObject.setClazz( className );
		if ( entityObject.isMetadataComplete() == null ) {
			entityObject.setMetadataComplete( defaults.isMetadataComplete() );
		}
		LOG.debugf( "Adding XML overriding information for %s", className );
	}

	private boolean hasSchemaOrCatalogDefined(EntityMappingsMocker.Default defaults) {
		return ( defaults != null ) && ( StringHelper.isNotEmpty( defaults.getSchema() ) || StringHelper.isNotEmpty(
				defaults.getCatalog()
		) );
	}

	private void applyDefaultCascadePersist(Map<DotName, List<AnnotationInstance>> annotationsMap) {
		for ( DotName annName : ASSOCIATION_ANNOTATIONS ) {
			if ( annotationsMap.containsKey( annName ) ) {
				addCascadePersistIfNotExist( annName, annotationsMap );
			}
		}
	}

	private void applyDefaultSchemaAndCatalog(Map<DotName, List<AnnotationInstance>> annotationsMap, EntityMappingsMocker.Default defaults) {
		for ( DotName annName : SCHEMA_AWARE_ANNOTATIONS ) {
			mockTableIfNonExist( annotationsMap, annName );
			if ( annotationsMap.containsKey( annName ) ) {
				overrideSchemaCatalogByDefault( annName, annotationsMap, defaults );
			}
		}
	}

	private void mockTableIfNonExist(Map<DotName, List<AnnotationInstance>> annotationsMap, DotName annName) {
		if ( annName == JPADotNames.TABLE && !annotationsMap.containsKey( JPADotNames.TABLE ) && annotationsMap
				.containsKey( JPADotNames.ENTITY ) ) {
			//if an entity doesn't have a @Table, we create one here
			AnnotationInstance entity = JandexHelper.getSingleAnnotation( annotationsMap, JPADotNames.ENTITY );
			AnnotationInstance table = MockHelper.create(
					JPADotNames.TABLE, entity.target(), MockHelper.EMPTY_ANNOTATION_VALUE_ARRAY
			);
			List<AnnotationInstance> annotationInstanceList = new ArrayList<AnnotationInstance>( 1 );
			annotationInstanceList.add( table );
			annotationsMap.put( JPADotNames.TABLE, annotationInstanceList );
		}
	}

	private void mockTableIfNonExist(JaxbEntity entity, EntityMappingsMocker.Default defaults) {
		if ( hasSchemaOrCatalogDefined( defaults ) ) {
			JaxbTable table = entity.getTable();
			if ( table == null ) {
				table = new JaxbTable();
				entity.setTable( table );
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
						continue;
					}
				}
				if ( hasPersistDefined ) {
					newAnnotationInstanceList.add( annotationInstance );
					continue;
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

			AnnotationInstance newAnnotationInstance = MockHelper.create(
					annotationInstance.name(),
					annotationInstance.target(),
					MockHelper.toArray( newAnnotationValueList )
			);
			newAnnotationInstanceList.add( newAnnotationInstance );
		}
		indexedAnnotationMap.put( annName, newAnnotationInstanceList );
	}

	//@Table, @CollectionTable, @JoinTable, @SecondaryTable
	private void overrideSchemaCatalogByDefault(DotName annName, Map<DotName, List<AnnotationInstance>> indexedAnnotationMap, EntityMappingsMocker.Default defaults) {
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
					newAnnotationValueArray[i] = MockHelper.nestedAnnotationValue(
							"", overrideSchemaCatalogByDefault(
							secondaryTableAnnotationInstanceArray[i],
							defaults
					)
					);
				}
				AnnotationInstance secondaryTablesAnnotationInstance = MockHelper.create(
						annName,
						annotationInstance.target(),
						new AnnotationValue[] {
								AnnotationValue.createArrayValue( "value", newAnnotationValueArray )
						}
				);
				newAnnotationInstanceList.add( secondaryTablesAnnotationInstance );
			}
			else {
				newAnnotationInstanceList.add( overrideSchemaCatalogByDefault( annotationInstance, defaults ) );
			}
		}
		indexedAnnotationMap.put( annName, newAnnotationInstanceList );
	}

	private AnnotationInstance overrideSchemaCatalogByDefault(AnnotationInstance annotationInstance, EntityMappingsMocker.Default defaults) {
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
		return MockHelper.create(
				annotationInstance.name(),
				annotationInstance.target(),
				MockHelper.toArray( newAnnotationValueList )
		);
	}

	private static interface EntityObject {
		String getClazz();

		void setClazz(String className);

		Boolean isMetadataComplete();

		void setMetadataComplete(Boolean isMetadataComplete);
	}

	private static class EntityEntityObject implements EntityObject {
		private JaxbEntity entity;

		private EntityEntityObject(JaxbEntity entity) {
			this.entity = entity;
		}

		@Override
		public String getClazz() {
			return entity.getClazz();
		}

		@Override
		public void setClazz(String className) {
			entity.setClazz( className );
		}

		@Override
		public Boolean isMetadataComplete() {
			return entity.isMetadataComplete();
		}

		@Override
		public void setMetadataComplete(Boolean isMetadataComplete) {
			entity.setMetadataComplete( isMetadataComplete );
		}
	}

	private static class EmbeddableEntityObject implements EntityObject {
		private JaxbEmbeddable entity;

		private EmbeddableEntityObject(JaxbEmbeddable entity) {
			this.entity = entity;
		}

		@Override
		public String getClazz() {
			return entity.getClazz();
		}

		@Override
		public void setClazz(String className) {
			entity.setClazz( className );
		}

		@Override
		public Boolean isMetadataComplete() {
			return entity.isMetadataComplete();
		}

		@Override
		public void setMetadataComplete(Boolean isMetadataComplete) {
			entity.setMetadataComplete( isMetadataComplete );
		}
	}

	private static class MappedSuperClassEntityObject implements EntityObject {
		private JaxbMappedSuperclass entity;

		private MappedSuperClassEntityObject(JaxbMappedSuperclass entity) {
			this.entity = entity;
		}

		@Override
		public String getClazz() {
			return entity.getClazz();
		}

		@Override
		public void setClazz(String className) {
			entity.setClazz( className );
		}

		@Override
		public Boolean isMetadataComplete() {
			return entity.isMetadataComplete();
		}

		@Override
		public void setMetadataComplete(Boolean isMetadataComplete) {
			entity.setMetadataComplete( isMetadataComplete );
		}
	}

}
