package org.hibernate.metamodel.source.annotations.xml.mocker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.annotation.xml.XMLAttributes;
import org.hibernate.metamodel.source.annotation.xml.XMLEntity;
import org.hibernate.metamodel.source.annotation.xml.XMLEntityMappings;
import org.hibernate.metamodel.source.annotation.xml.XMLId;
import org.hibernate.metamodel.source.annotation.xml.XMLNamedNativeQuery;
import org.hibernate.metamodel.source.annotation.xml.XMLNamedQuery;
import org.hibernate.metamodel.source.annotation.xml.XMLSequenceGenerator;
import org.hibernate.metamodel.source.annotation.xml.XMLSqlResultSetMapping;
import org.hibernate.metamodel.source.annotation.xml.XMLTableGenerator;
import org.hibernate.metamodel.source.annotations.JPADotNames;

/**
 * @author Strong Liu
 */
class GlobalAnnotations implements JPADotNames {
	private Map<String, XMLSequenceGenerator> sequenceGeneratorMap = new HashMap<String, XMLSequenceGenerator>();
	private Map<String, XMLTableGenerator> tableGeneratorMap = new HashMap<String, XMLTableGenerator>();
	private Map<String, XMLNamedQuery> namedQueryMap = new HashMap<String, XMLNamedQuery>();
	private Map<String, XMLNamedNativeQuery> namedNativeQueryMap = new HashMap<String, XMLNamedNativeQuery>();
	private Map<String, XMLSqlResultSetMapping> sqlResultSetMappingMap = new HashMap<String, XMLSqlResultSetMapping>();
	private Map<DotName, List<AnnotationInstance>> annotationInstanceMap = new HashMap<DotName, List<AnnotationInstance>>();
	private List<AnnotationInstance> indexedAnnotationInstanceList = new ArrayList<AnnotationInstance>();

	Map<DotName, List<AnnotationInstance>> getAnnotationInstanceMap() {
		return annotationInstanceMap;
	}

	AnnotationInstance push(DotName name, AnnotationInstance annotationInstance) {
		if ( name == null || annotationInstance == null ) {
			return null;
		}
		List<AnnotationInstance> list = annotationInstanceMap.get( name );
		if ( list == null ) {
			list = new ArrayList<AnnotationInstance>();
		}
		list.add( annotationInstance );
		return annotationInstance;
	}


	void addIndexedAnnotationInstance(List<AnnotationInstance> annotationInstanceList) {
		if ( MockHelper.isNotEmpty( annotationInstanceList ) ) {
			indexedAnnotationInstanceList.addAll( annotationInstanceList );
		}
	}

	/**
	 * do the orm xmls define global configurations?
	 */
	boolean hasGlobalConfiguration() {
		return !( namedQueryMap.isEmpty() && namedNativeQueryMap.isEmpty() && sequenceGeneratorMap.isEmpty() && tableGeneratorMap
				.isEmpty() && sqlResultSetMappingMap.isEmpty() );
	}

	Map<String, XMLNamedNativeQuery> getNamedNativeQueryMap() {
		return namedNativeQueryMap;
	}

	Map<String, XMLNamedQuery> getNamedQueryMap() {
		return namedQueryMap;
	}

	Map<String, XMLSequenceGenerator> getSequenceGeneratorMap() {
		return sequenceGeneratorMap;
	}

	Map<String, XMLSqlResultSetMapping> getSqlResultSetMappingMap() {
		return sqlResultSetMappingMap;
	}

	Map<String, XMLTableGenerator> getTableGeneratorMap() {
		return tableGeneratorMap;
	}

	void put(String name, XMLNamedNativeQuery value) {
		if ( value != null ) {
			namedNativeQueryMap.put( name, value );
		}
	}

	void put(String name, XMLNamedQuery value) {
		if ( value != null ) {
			namedQueryMap.put( name, value );
		}
	}

	void put(String name, XMLSequenceGenerator value) {
		if ( value != null ) {
			sequenceGeneratorMap.put( name, value );
		}
	}

	void put(String name, XMLTableGenerator value) {
		if ( value != null ) {
			tableGeneratorMap.put( name, value );
		}
	}

	void put(String name, XMLSqlResultSetMapping value) {
		if ( value != null ) {
			sqlResultSetMappingMap.put( name, value );
		}
	}

	public void filterIndexedAnnotations() {
		for ( AnnotationInstance annotationInstance : indexedAnnotationInstanceList ) {
			pushIfNotExist( annotationInstance );
		}

	}

	private void pushIfNotExist(AnnotationInstance annotationInstance) {
		DotName annName = annotationInstance.name();
		boolean isNotExist = false;
		if ( annName.equals( SQL_RESULT_SET_MAPPINGS ) ) {
			AnnotationInstance[] annotationInstances = annotationInstance.value().asNestedArray();
			for ( AnnotationInstance ai : annotationInstances ) {
				pushIfNotExist( ai );
			}
		}
		else {
			AnnotationValue value = annotationInstance.value( "name" );
			String name = value.asString();
			isNotExist = ( annName.equals( TABLE_GENERATOR ) && !tableGeneratorMap.containsKey( name ) ) ||
					( annName.equals( SEQUENCE_GENERATOR ) && !sequenceGeneratorMap.containsKey( name ) ) ||
					( annName.equals( NAMED_QUERY ) && !namedQueryMap.containsKey( name ) ) ||
					( annName.equals( NAMED_NATIVE_QUERY ) && !namedNativeQueryMap.containsKey( name ) ) ||
					( annName.equals( SQL_RESULT_SET_MAPPING ) && !sqlResultSetMappingMap.containsKey( name ) );
		}
		if ( isNotExist ) {
			push( annName, annotationInstance );
		}

	}

	void collectGlobalMappings(XMLEntityMappings entityMappings, EntityMappingsMocker.Default defaults) {
		for ( XMLSequenceGenerator generator : entityMappings.getSequenceGenerator() ) {
			put( generator.getName(), overrideGenerator( generator, defaults ) );
		}
		for ( XMLTableGenerator generator : entityMappings.getTableGenerator() ) {
			put( generator.getName(), overrideGenerator( generator, defaults ) );
		}
		for ( XMLNamedQuery namedQuery : entityMappings.getNamedQuery() ) {
			put( namedQuery.getName(), namedQuery );
		}
		for ( XMLNamedNativeQuery namedNativeQuery : entityMappings.getNamedNativeQuery() ) {
			put( namedNativeQuery.getName(), namedNativeQuery );
		}
		for ( XMLSqlResultSetMapping sqlResultSetMapping : entityMappings.getSqlResultSetMapping() ) {
			put( sqlResultSetMapping.getName(), sqlResultSetMapping );
		}
	}

	void collectGlobalMappings(XMLEntity entity, EntityMappingsMocker.Default defaults) {
		for ( XMLNamedQuery namedQuery : entity.getNamedQuery() ) {
			put( namedQuery.getName(), namedQuery );
		}
		for ( XMLNamedNativeQuery namedNativeQuery : entity.getNamedNativeQuery() ) {
			put( namedNativeQuery.getName(), namedNativeQuery );
		}
		for ( XMLSqlResultSetMapping sqlResultSetMapping : entity.getSqlResultSetMapping() ) {
			put( sqlResultSetMapping.getName(), sqlResultSetMapping );
		}
		XMLSequenceGenerator sequenceGenerator = entity.getSequenceGenerator();
		if ( sequenceGenerator != null ) {
			put( sequenceGenerator.getName(), overrideGenerator( sequenceGenerator, defaults ) );
		}
		XMLTableGenerator tableGenerator = entity.getTableGenerator();
		if ( tableGenerator != null ) {
			put( tableGenerator.getName(), overrideGenerator( tableGenerator, defaults ) );
		}
		XMLAttributes attributes = entity.getAttributes();
		if ( attributes != null ) {
			for ( XMLId id : attributes.getId() ) {
				sequenceGenerator = id.getSequenceGenerator();
				if ( sequenceGenerator != null ) {
					put(
							sequenceGenerator.getName(), overrideGenerator(
							sequenceGenerator, defaults
					)
					);
				}
				tableGenerator = id.getTableGenerator();
				if ( tableGenerator != null ) {
					put( tableGenerator.getName(), overrideGenerator( tableGenerator, defaults ) );
				}
			}
		}
	}

	/**
	 * Override SequenceGenerator using info definded in EntityMappings/Persistence-Metadata-Unit
	 */
	private static XMLSequenceGenerator overrideGenerator(XMLSequenceGenerator generator, EntityMappingsMocker.Default defaults) {
		if ( StringHelper.isEmpty( generator.getSchema() ) ) {
			generator.setSchema( defaults.getSchema() );
		}
		if ( StringHelper.isEmpty( generator.getCatalog() ) ) {
			generator.setCatalog( defaults.getCatalog() );
		}
		return generator;
	}

	/**
	 * Override TableGenerator using info definded in EntityMappings/Persistence-Metadata-Unit
	 */
	private static XMLTableGenerator overrideGenerator(XMLTableGenerator generator, EntityMappingsMocker.Default defaults) {
		if ( StringHelper.isEmpty( generator.getSchema() ) ) {
			generator.setSchema( defaults.getSchema() );
		}
		if ( StringHelper.isEmpty( generator.getCatalog() ) ) {
			generator.setCatalog( defaults.getCatalog() );
		}
		return generator;
	}


}
