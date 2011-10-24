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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.jaxb.mapping.orm.JaxbAttributes;
import org.hibernate.internal.jaxb.mapping.orm.JaxbEntity;
import org.hibernate.internal.jaxb.mapping.orm.JaxbEntityMappings;
import org.hibernate.internal.jaxb.mapping.orm.JaxbId;
import org.hibernate.internal.jaxb.mapping.orm.JaxbNamedNativeQuery;
import org.hibernate.internal.jaxb.mapping.orm.JaxbNamedQuery;
import org.hibernate.internal.jaxb.mapping.orm.JaxbSequenceGenerator;
import org.hibernate.internal.jaxb.mapping.orm.JaxbSqlResultSetMapping;
import org.hibernate.internal.jaxb.mapping.orm.JaxbTableGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.MappingException;
import org.hibernate.metamodel.source.annotations.JPADotNames;

/**
 * @author Strong Liu
 */
class GlobalAnnotations implements JPADotNames {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			GlobalAnnotations.class.getName()
	);
	private Map<String, JaxbSequenceGenerator> sequenceGeneratorMap = new HashMap<String, JaxbSequenceGenerator>();
	private Map<String, JaxbTableGenerator> tableGeneratorMap = new HashMap<String, JaxbTableGenerator>();
	private Map<String, JaxbNamedQuery> namedQueryMap = new HashMap<String, JaxbNamedQuery>();
	private Map<String, JaxbNamedNativeQuery> namedNativeQueryMap = new HashMap<String, JaxbNamedNativeQuery>();
	private Map<String, JaxbSqlResultSetMapping> sqlResultSetMappingMap = new HashMap<String, JaxbSqlResultSetMapping>();
	private Map<DotName, List<AnnotationInstance>> annotationInstanceMap = new HashMap<DotName, List<AnnotationInstance>>();
	private List<AnnotationInstance> indexedAnnotationInstanceList = new ArrayList<AnnotationInstance>();
	//---------------------------
	private Set<String> defaultNamedNativeQueryNames = new HashSet<String>();
	private Set<String> defaultNamedQueryNames = new HashSet<String>();
	private Set<String> defaultNamedGenerators = new HashSet<String>();
	private Set<String> defaultSqlResultSetMappingNames = new HashSet<String>();

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
			annotationInstanceMap.put( name, list );
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

	Map<String, JaxbNamedNativeQuery> getNamedNativeQueryMap() {
		return namedNativeQueryMap;
	}

	Map<String, JaxbNamedQuery> getNamedQueryMap() {
		return namedQueryMap;
	}

	Map<String, JaxbSequenceGenerator> getSequenceGeneratorMap() {
		return sequenceGeneratorMap;
	}

	Map<String, JaxbSqlResultSetMapping> getSqlResultSetMappingMap() {
		return sqlResultSetMappingMap;
	}

	Map<String, JaxbTableGenerator> getTableGeneratorMap() {
		return tableGeneratorMap;
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

	void collectGlobalMappings(JaxbEntityMappings entityMappings, EntityMappingsMocker.Default defaults) {
		for ( JaxbSequenceGenerator generator : entityMappings.getSequenceGenerator() ) {
			put( generator, defaults );
			defaultNamedGenerators.add( generator.getName() );
		}
		for ( JaxbTableGenerator generator : entityMappings.getTableGenerator() ) {
			put( generator, defaults );
			defaultNamedGenerators.add( generator.getName() );
		}
		for ( JaxbNamedQuery namedQuery : entityMappings.getNamedQuery() ) {
			put( namedQuery );
			defaultNamedQueryNames.add( namedQuery.getName() );
		}
		for ( JaxbNamedNativeQuery namedNativeQuery : entityMappings.getNamedNativeQuery() ) {
			put( namedNativeQuery );
			defaultNamedNativeQueryNames.add( namedNativeQuery.getName() );
		}
		for ( JaxbSqlResultSetMapping sqlResultSetMapping : entityMappings.getSqlResultSetMapping() ) {
			put( sqlResultSetMapping );
			defaultSqlResultSetMappingNames.add( sqlResultSetMapping.getName() );
		}
	}

	void collectGlobalMappings(JaxbEntity entity, EntityMappingsMocker.Default defaults) {
		for ( JaxbNamedQuery namedQuery : entity.getNamedQuery() ) {
			if ( !defaultNamedQueryNames.contains( namedQuery.getName() ) ) {
				put( namedQuery );
			}
			else {
				LOG.warn( "Named Query [" + namedQuery.getName() + "] duplicated." );
			}
		}
		for ( JaxbNamedNativeQuery namedNativeQuery : entity.getNamedNativeQuery() ) {
			if ( !defaultNamedNativeQueryNames.contains( namedNativeQuery.getName() ) ) {
				put( namedNativeQuery );
			}
			else {
				LOG.warn( "Named native Query [" + namedNativeQuery.getName() + "] duplicated." );
			}
		}
		for ( JaxbSqlResultSetMapping sqlResultSetMapping : entity.getSqlResultSetMapping() ) {
			if ( !defaultSqlResultSetMappingNames.contains( sqlResultSetMapping.getName() ) ) {
				put( sqlResultSetMapping );
			}
		}
		JaxbSequenceGenerator sequenceGenerator = entity.getSequenceGenerator();
		if ( sequenceGenerator != null ) {
			if ( !defaultNamedGenerators.contains( sequenceGenerator.getName() ) ) {
				put( sequenceGenerator, defaults );
			}
		}
		JaxbTableGenerator tableGenerator = entity.getTableGenerator();
		if ( tableGenerator != null ) {
			if ( !defaultNamedGenerators.contains( tableGenerator.getName() ) ) {
				put( tableGenerator, defaults );
			}
		}
		JaxbAttributes attributes = entity.getAttributes();
		if ( attributes != null ) {
			for ( JaxbId id : attributes.getId() ) {
				sequenceGenerator = id.getSequenceGenerator();
				if ( sequenceGenerator != null ) {
					put( sequenceGenerator, defaults );
				}
				tableGenerator = id.getTableGenerator();
				if ( tableGenerator != null ) {
					put( tableGenerator, defaults );
				}
			}
		}
	}

	/**
	 * Override SequenceGenerator using info definded in EntityMappings/Persistence-Metadata-Unit
	 */
	private static JaxbSequenceGenerator overrideGenerator(JaxbSequenceGenerator generator, EntityMappingsMocker.Default defaults) {
		if ( StringHelper.isEmpty( generator.getSchema() ) && defaults != null ) {
			generator.setSchema( defaults.getSchema() );
		}
		if ( StringHelper.isEmpty( generator.getCatalog() ) && defaults != null ) {
			generator.setCatalog( defaults.getCatalog() );
		}
		return generator;
	}

	/**
	 * Override TableGenerator using info definded in EntityMappings/Persistence-Metadata-Unit
	 */
	private static JaxbTableGenerator overrideGenerator(JaxbTableGenerator generator, EntityMappingsMocker.Default defaults) {
		if ( StringHelper.isEmpty( generator.getSchema() ) && defaults != null ) {
			generator.setSchema( defaults.getSchema() );
		}
		if ( StringHelper.isEmpty( generator.getCatalog() ) && defaults != null ) {
			generator.setCatalog( defaults.getCatalog() );
		}
		return generator;
	}

	private void put(JaxbNamedNativeQuery query) {
		if ( query != null ) {
			checkQueryName( query.getName() );
			namedNativeQueryMap.put( query.getName(), query );
		}
	}

	private void checkQueryName(String name) {
		if ( namedQueryMap.containsKey( name ) || namedNativeQueryMap.containsKey( name ) ) {
			throw new MappingException( "Duplicated query mapping " + name, null );
		}
	}

	private void put(JaxbNamedQuery query) {
		if ( query != null ) {
			checkQueryName( query.getName() );
			namedQueryMap.put( query.getName(), query );
		}
	}

	private void put(JaxbSequenceGenerator generator, EntityMappingsMocker.Default defaults) {
		if ( generator != null ) {
			Object old = sequenceGeneratorMap.put( generator.getName(), overrideGenerator( generator, defaults ) );
			if ( old != null ) {
				LOG.duplicateGeneratorName( generator.getName() );
			}
		}
	}

	private void put(JaxbTableGenerator generator, EntityMappingsMocker.Default defaults) {
		if ( generator != null ) {
			Object old = tableGeneratorMap.put( generator.getName(), overrideGenerator( generator, defaults ) );
			if ( old != null ) {
				LOG.duplicateGeneratorName( generator.getName() );
			}
		}
	}

	private void put(JaxbSqlResultSetMapping mapping) {
		if ( mapping != null ) {
			Object old = sqlResultSetMappingMap.put( mapping.getName(), mapping );
			if ( old != null ) {
				throw new MappingException( "Duplicated SQL result set mapping " +  mapping.getName(), null );
			}
		}
	}
}
