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
package org.hibernate.metamodel.source.internal.jandex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.jaxb.JaxbAttributes;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntity;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntityMappings;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmFetchProfile;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmFetchProfile.JaxbFetch;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmFilterDef;
import org.hibernate.metamodel.source.internal.jaxb.JaxbId;
import org.hibernate.metamodel.source.internal.jaxb.JaxbNamedNativeQuery;
import org.hibernate.metamodel.source.internal.jaxb.JaxbNamedQuery;
import org.hibernate.metamodel.source.internal.jaxb.JaxbSequenceGenerator;
import org.hibernate.metamodel.source.internal.jaxb.JaxbSqlResultSetMapping;
import org.hibernate.metamodel.source.internal.jaxb.JaxbTableGenerator;
import org.hibernate.metamodel.source.internal.jaxb.SchemaAware;
import org.hibernate.metamodel.source.spi.MappingException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

/**
 * @author Strong Liu
 */
// TODO: Much of this class is unnecessary -- use simple lists and let duplication be checked later on?
public class GlobalAnnotations implements JPADotNames {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( GlobalAnnotations.class );

	private final Map<String, JaxbSequenceGenerator> sequenceGeneratorMap = new HashMap<String, JaxbSequenceGenerator>();
	private final Map<String, JaxbTableGenerator> tableGeneratorMap = new HashMap<String, JaxbTableGenerator>();
	private final Map<String, JaxbNamedQuery> namedQueryMap = new HashMap<String, JaxbNamedQuery>();
	private final Map<String, JaxbNamedNativeQuery> namedNativeQueryMap = new HashMap<String, JaxbNamedNativeQuery>();
	private final Map<String, JaxbSqlResultSetMapping> sqlResultSetMappingMap = new HashMap<String, JaxbSqlResultSetMapping>();
	private final Map<DotName, List<AnnotationInstance>> annotationInstanceMap = new HashMap<DotName, List<AnnotationInstance>>();
	private final List<AnnotationInstance> indexedAnnotationInstanceList = new ArrayList<AnnotationInstance>();
	private final Map<String, JaxbHbmFilterDef> filterDefMap = new HashMap<String, JaxbHbmFilterDef>();
	private final Map<String, JaxbHbmFetchProfile> fetchProfileMap = new HashMap<String, JaxbHbmFetchProfile>();
	//---------------------------
	private final Set<String> defaultNamedNativeQueryNames = new HashSet<String>();
	private final Set<String> defaultNamedQueryNames = new HashSet<String>();
	private final Set<String> defaultNamedGenerators = new HashSet<String>();
	private final Set<String> defaultSqlResultSetMappingNames = new HashSet<String>();

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


	void addIndexedAnnotationInstance(Collection<AnnotationInstance> annotationInstanceList) {
		if ( CollectionHelper.isNotEmpty( annotationInstanceList ) ) {
			indexedAnnotationInstanceList.addAll( annotationInstanceList );
		}
	}

	/**
	 * do the orm xmls define global configurations?
	 */
	boolean hasGlobalConfiguration() {
		return !( namedQueryMap.isEmpty()
				&& namedNativeQueryMap.isEmpty()
				&& sequenceGeneratorMap.isEmpty()
				&& tableGeneratorMap.isEmpty()
				&& sqlResultSetMappingMap.isEmpty()
				&& filterDefMap.isEmpty()
				&& fetchProfileMap.isEmpty() );
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

	Map<String, JaxbHbmFilterDef> getFilterDefMap() {
		return filterDefMap;
	}

	Map<String, JaxbHbmFetchProfile> getFetchProfileMap() {
		return fetchProfileMap;
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
			if ( value != null ) {
				String name = value.asString();
				isNotExist = ( annName.equals( TABLE_GENERATOR ) && !tableGeneratorMap.containsKey( name ) ) ||
						( annName.equals( SEQUENCE_GENERATOR ) && !sequenceGeneratorMap.containsKey( name ) ) ||
						( annName.equals( NAMED_QUERY ) && !namedQueryMap.containsKey( name ) ) ||
						( annName.equals( NAMED_NATIVE_QUERY ) && !namedNativeQueryMap.containsKey( name ) ) ||
						( annName.equals( SQL_RESULT_SET_MAPPING ) && !sqlResultSetMappingMap.containsKey( name ) ) ||
						( annName.equals( HibernateDotNames.FILTER_DEF ) && !filterDefMap.containsKey( name ) ) ||
						( annName.equals( HibernateDotNames.FETCH_PROFILE ) && !fetchProfileMap.containsKey( name ) );
			}
		}
		if ( isNotExist ) {
			push( annName, annotationInstance );
		}
	}

	void collectGlobalMappings(JaxbEntityMappings entityMappings, Default defaults) {
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
		for ( JaxbHbmFilterDef filterDef : entityMappings.getFilterDef() ) {
			if (filterDef != null) {
				filterDefMap.put( filterDef.getName(), filterDef );
			}
		}
		for ( JaxbHbmFetchProfile fetchProfile : entityMappings.getFetchProfile() ) {
			put( fetchProfile, entityMappings.getPackage(), null );
		}
	}

	void collectGlobalMappings(JaxbEntity entity, Default defaults) {
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
		
		for (JaxbHbmFetchProfile fetchProfile : entity.getFetchProfile()) {
			put( fetchProfile, defaults.getPackageName(), entity.getClazz() );
		}
	}

	/**
	 * Override SequenceGenerator using info definded in EntityMappings/Persistence-Metadata-Unit
	 */
	private static void overrideGenerator(SchemaAware generator, Default defaults) {
		if ( StringHelper.isEmpty( generator.getSchema() ) && defaults != null ) {
			generator.setSchema( defaults.getSchema() );
		}
		if ( StringHelper.isEmpty( generator.getCatalog() ) && defaults != null ) {
			generator.setCatalog( defaults.getCatalog() );
		}
	}

	private void checkQueryName(String name) {
		if ( namedQueryMap.containsKey( name ) || namedNativeQueryMap.containsKey( name ) ) {
			throw new MappingException( "Duplicated query mapping " + name, null );
		}
	}
	private void checkDuplicated(Object old, String name){
		if ( old != null ) {
			LOG.duplicateGeneratorName( name );
		}
	}

	private void put(JaxbNamedQuery query) {
		if ( query != null ) {
			checkQueryName( query.getName() );
			namedQueryMap.put( query.getName(), query );
		}
	}

	private void put(JaxbSequenceGenerator generator, Default defaults) {
		if ( generator != null ) {
			overrideGenerator( generator, defaults );
			Object old = sequenceGeneratorMap.put( generator.getName(), generator );
			checkDuplicated( old, generator.getName() );
		}
	}

	private void put(JaxbTableGenerator generator, Default defaults) {
		if ( generator != null ) {
			overrideGenerator( generator, defaults );
			Object old = tableGeneratorMap.put( generator.getName(), generator );
			checkDuplicated( old, generator.getName() );
		}
	}
	private void put(JaxbNamedNativeQuery query) {
		if ( query != null ) {
			checkQueryName( query.getName() );
			namedNativeQueryMap.put( query.getName(), query );
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
	
	public void put(JaxbHbmFetchProfile fetchProfile, String packageName, String defaultClassName) {
		if (fetchProfile != null) {
			for (JaxbFetch fetch : fetchProfile.getFetch()) {
				String entityName = StringHelper.isEmpty( fetch.getEntity() ) ? defaultClassName : fetch.getEntity();
				fetch.setEntity( MockHelper.buildSafeClassName( entityName, packageName ) );
			}
			fetchProfileMap.put( fetchProfile.getName(), fetchProfile );
		}
	}
}
