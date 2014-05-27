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
import java.util.List;
import java.util.Map;

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
import org.jboss.jandex.DotName;

/**
 * @author Strong Liu
 * @author Brett Meyer
 */
public class GlobalAnnotations implements JPADotNames {
	private final List<JaxbSequenceGenerator> sequenceGenerators = new ArrayList<JaxbSequenceGenerator>();
	private final List<JaxbTableGenerator> tableGenerators = new ArrayList<JaxbTableGenerator>();
	private final List<JaxbNamedQuery> namedQuerys = new ArrayList<JaxbNamedQuery>();
	private final List<JaxbNamedNativeQuery> namedNativeQuerys = new ArrayList<JaxbNamedNativeQuery>();
	private final List<JaxbSqlResultSetMapping> sqlResultSetMappings = new ArrayList<JaxbSqlResultSetMapping>();
	private final List<JaxbHbmFilterDef> filterDefs = new ArrayList<JaxbHbmFilterDef>();
	private final List<JaxbHbmFetchProfile> fetchProfiles = new ArrayList<JaxbHbmFetchProfile>();
	
	private final List<AnnotationInstance> indexedAnnotationInstances = new ArrayList<AnnotationInstance>();
	private final Map<DotName, List<AnnotationInstance>> annotationInstances = new HashMap<DotName, List<AnnotationInstance>>();

	Map<DotName, List<AnnotationInstance>> getAnnotationInstanceMap() {
		return annotationInstances;
	}

	AnnotationInstance push(DotName name, AnnotationInstance annotationInstance) {
		if ( name == null || annotationInstance == null ) {
			return null;
		}
		List<AnnotationInstance> list = annotationInstances.get( name );
		if ( list == null ) {
			list = new ArrayList<AnnotationInstance>();
			annotationInstances.put( name, list );
		}
		list.add( annotationInstance );
		return annotationInstance;
	}


	void addIndexedAnnotationInstance(Collection<AnnotationInstance> annotationInstanceList) {
		if ( CollectionHelper.isNotEmpty( annotationInstanceList ) ) {
			indexedAnnotationInstances.addAll( annotationInstanceList );
		}
	}

	/**
	 * do the orm xmls define global configurations?
	 */
	boolean hasGlobalConfiguration() {
		return !( namedQuerys.isEmpty()
				&& namedNativeQuerys.isEmpty()
				&& sequenceGenerators.isEmpty()
				&& tableGenerators.isEmpty()
				&& sqlResultSetMappings.isEmpty()
				&& filterDefs.isEmpty()
				&& fetchProfiles.isEmpty() );
	}

	List<JaxbNamedNativeQuery> getNamedNativeQueries() {
		return namedNativeQuerys;
	}

	List<JaxbNamedQuery> getNamedQueries() {
		return namedQuerys;
	}

	List<JaxbSequenceGenerator> getSequenceGenerators() {
		return sequenceGenerators;
	}

	List<JaxbSqlResultSetMapping> getSqlResultSetMappings() {
		return sqlResultSetMappings;
	}

	List<JaxbTableGenerator> getTableGenerators() {
		return tableGenerators;
	}

	List<JaxbHbmFilterDef> getFilterDefs() {
		return filterDefs;
	}

	List<JaxbHbmFetchProfile> getFetchProfiles() {
		return fetchProfiles;
	}


	public void filterIndexedAnnotations() {
		for ( AnnotationInstance annotationInstance : indexedAnnotationInstances ) {
			push( annotationInstance );
		}
	}

	private void push(AnnotationInstance annotationInstance) {
		DotName annName = annotationInstance.name();
		if ( annName.equals( SQL_RESULT_SET_MAPPINGS )
				|| annName.equals( NAMED_NATIVE_QUERIES )
				|| annName.equals( NAMED_QUERIES )
				|| annName.equals( HibernateDotNames.FILTER_DEFS )
				|| annName.equals( HibernateDotNames.FETCH_PROFILES )) {
			AnnotationInstance[] annotationInstances = annotationInstance.value().asNestedArray();
			for ( AnnotationInstance ai : annotationInstances ) {
				push( ai );
			}
		}
		else {
			push( annName, annotationInstance );
		}
	}

	void collectGlobalMappings(JaxbEntityMappings entityMappings, Default defaults) {
		for ( JaxbSequenceGenerator generator : entityMappings.getSequenceGenerator() ) {
			put( generator, defaults );
		}
		for ( JaxbTableGenerator generator : entityMappings.getTableGenerator() ) {
			put( generator, defaults );
		}
		for ( JaxbNamedQuery namedQuery : entityMappings.getNamedQuery() ) {
			put( namedQuery );
		}
		for ( JaxbNamedNativeQuery namedNativeQuery : entityMappings.getNamedNativeQuery() ) {
			put( namedNativeQuery );
		}
		for ( JaxbSqlResultSetMapping sqlResultSetMapping : entityMappings.getSqlResultSetMapping() ) {
			put( sqlResultSetMapping );
		}
		for ( JaxbHbmFilterDef filterDef : entityMappings.getFilterDef() ) {
			if (filterDef != null) {
				filterDefs.add( filterDef );
			}
		}
		for ( JaxbHbmFetchProfile fetchProfile : entityMappings.getFetchProfile() ) {
			put( fetchProfile, entityMappings.getPackage(), null );
		}
	}

	void collectGlobalMappings(JaxbEntity entity, Default defaults) {
		for ( JaxbNamedQuery namedQuery : entity.getNamedQuery() ) {
			put( namedQuery );
		}
		for ( JaxbNamedNativeQuery namedNativeQuery : entity.getNamedNativeQuery() ) {
			put( namedNativeQuery );
		}
		for ( JaxbSqlResultSetMapping sqlResultSetMapping : entity.getSqlResultSetMapping() ) {
			put( sqlResultSetMapping );
		}
		JaxbSequenceGenerator sequenceGenerator = entity.getSequenceGenerator();
		if ( sequenceGenerator != null ) {
			put( sequenceGenerator, defaults );
		}
		JaxbTableGenerator tableGenerator = entity.getTableGenerator();
		if ( tableGenerator != null ) {
			put( tableGenerator, defaults );
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

	private void put(JaxbNamedQuery query) {
		if ( query != null ) {
			namedQuerys.add( query );
		}
	}

	private void put(JaxbSequenceGenerator generator, Default defaults) {
		if ( generator != null ) {
			overrideGenerator( generator, defaults );
		}
	}

	private void put(JaxbTableGenerator generator, Default defaults) {
		if ( generator != null ) {
			overrideGenerator( generator, defaults );
		}
	}
	private void put(JaxbNamedNativeQuery query) {
		if ( query != null ) {
			namedNativeQuerys.add( query );
		}
	}

	private void put(JaxbSqlResultSetMapping mapping) {
		if ( mapping != null ) {
			Object old = sqlResultSetMappings.add( mapping );
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
			fetchProfiles.add( fetchProfile );
		}
	}
}
