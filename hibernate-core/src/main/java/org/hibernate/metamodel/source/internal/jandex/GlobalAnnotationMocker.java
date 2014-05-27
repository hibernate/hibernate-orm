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
import java.util.List;

import org.hibernate.annotations.FetchMode;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmFetchProfile;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmFetchProfile.JaxbFetch;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmFilterDef;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmFilterDef.JaxbFilterParam;
import org.hibernate.metamodel.source.internal.jaxb.JaxbNamedNativeQuery;
import org.hibernate.metamodel.source.internal.jaxb.JaxbNamedQuery;
import org.hibernate.metamodel.source.internal.jaxb.JaxbQueryHint;
import org.hibernate.metamodel.source.internal.jaxb.JaxbSequenceGenerator;
import org.hibernate.metamodel.source.internal.jaxb.JaxbSqlResultSetMapping;
import org.hibernate.metamodel.source.internal.jaxb.JaxbSqlResultSetMappingColumnResult;
import org.hibernate.metamodel.source.internal.jaxb.JaxbSqlResultSetMappingEntityResult;
import org.hibernate.metamodel.source.internal.jaxb.JaxbSqlResultSetMappingFieldResult;
import org.hibernate.metamodel.source.internal.jaxb.JaxbTableGenerator;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

/**
 * @author Strong Liu
 * @author Brett Meyer
 */
public class GlobalAnnotationMocker extends AbstractMocker {
	private GlobalAnnotations globalAnnotations;

	GlobalAnnotationMocker(IndexBuilder indexBuilder, GlobalAnnotations globalAnnotations, Default defaults) {
		super( indexBuilder, defaults );
		this.globalAnnotations = globalAnnotations;
	}

	void process() {
		if ( !globalAnnotations.getTableGenerators().isEmpty() ) {
			for ( JaxbTableGenerator generator : globalAnnotations.getTableGenerators() ) {
				parseTableGenerator( generator );
			}
		}
		if ( !globalAnnotations.getSequenceGenerators().isEmpty() ) {
			for ( JaxbSequenceGenerator generator : globalAnnotations.getSequenceGenerators() ) {
				parseSequenceGenerator( generator );
			}
		}
		if ( !globalAnnotations.getNamedQueries().isEmpty() ) {
			parseNamedQueries( globalAnnotations.getNamedQueries() );
		}
		if ( !globalAnnotations.getNamedNativeQueries().isEmpty() ) {
			parseNamedNativeQueries( globalAnnotations.getNamedNativeQueries() );
		}
		if ( !globalAnnotations.getSqlResultSetMappings().isEmpty() ) {
			parseSqlResultSetMappings( globalAnnotations.getSqlResultSetMappings() );
		}
		if ( !globalAnnotations.getFilterDefs().isEmpty() ) {
			parseFilterDefs( globalAnnotations.getFilterDefs() );
		}
		if ( !globalAnnotations.getFetchProfiles().isEmpty() ) {
			parseFetchProfiles( globalAnnotations.getFetchProfiles() );
		}
		indexBuilder.finishGlobalConfigurationMocking( globalAnnotations );
	}

	private AnnotationInstance parseSqlResultSetMappings(Collection<JaxbSqlResultSetMapping> namedQueries) {
		AnnotationValue[] values = new AnnotationValue[namedQueries.size()];
		int i = 0;
		for ( JaxbSqlResultSetMapping namedQuery : namedQueries ) {
			AnnotationInstance annotationInstance = parseSqlResultSetMapping( namedQuery );
			values[i++] = MockHelper.nestedAnnotationValue(
					"", annotationInstance
			);
		}
		return create(
				SQL_RESULT_SET_MAPPINGS, null,
				new AnnotationValue[] { AnnotationValue.createArrayValue( "value", values ) }

		);
	}


	//@SqlResultSetMapping
	private AnnotationInstance parseSqlResultSetMapping(JaxbSqlResultSetMapping mapping) {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", mapping.getName(), annotationValueList );
		nestedEntityResultList( "entities", mapping.getEntityResult(), annotationValueList );
		nestedColumnResultList( "columns", mapping.getColumnResult(), annotationValueList );
		return create( SQL_RESULT_SET_MAPPING, null, annotationValueList );
	}


	//@EntityResult
	private AnnotationInstance parseEntityResult(JaxbSqlResultSetMappingEntityResult result) {

		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue(
				"discriminatorColumn", result.getDiscriminatorColumn(), annotationValueList
		);
		nestedFieldResultList( "fields", result.getFieldResult(), annotationValueList );
		MockHelper.classValue( "entityClass", result.getEntityClass(), annotationValueList, getDefaults(),
				indexBuilder.getServiceRegistry() );
		return
				create(
						ENTITY_RESULT, null, annotationValueList

				);
	}

	private void nestedEntityResultList(String name, List<JaxbSqlResultSetMappingEntityResult> entityResults, List<AnnotationValue> annotationValueList) {
		if ( CollectionHelper.isNotEmpty( entityResults ) ) {
			AnnotationValue[] values = new AnnotationValue[entityResults.size()];
			for ( int i = 0; i < entityResults.size(); i++ ) {
				AnnotationInstance annotationInstance = parseEntityResult( entityResults.get( i ) );
				values[i] = MockHelper.nestedAnnotationValue(
						"", annotationInstance
				);
			}
			MockHelper.addToCollectionIfNotNull(
					annotationValueList, AnnotationValue.createArrayValue( name, values )
			);
		}
	}

	//@ColumnResult
	private AnnotationInstance parseColumnResult(JaxbSqlResultSetMappingColumnResult result) {
		return create( COLUMN_RESULT, null, MockHelper.stringValueArray( "name", result.getName() ) );
	}

	private void nestedColumnResultList(String name, List<JaxbSqlResultSetMappingColumnResult> columnResults, List<AnnotationValue> annotationValueList) {
		if ( CollectionHelper.isNotEmpty( columnResults ) ) {
			AnnotationValue[] values = new AnnotationValue[columnResults.size()];
			for ( int i = 0; i < columnResults.size(); i++ ) {
				AnnotationInstance annotationInstance = parseColumnResult( columnResults.get( i ) );
				values[i] = MockHelper.nestedAnnotationValue(
						"", annotationInstance
				);
			}
			MockHelper.addToCollectionIfNotNull(
					annotationValueList, AnnotationValue.createArrayValue( name, values )
			);
		}
	}

	//@FieldResult
	private AnnotationInstance parseFieldResult(JaxbSqlResultSetMappingFieldResult result) {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", result.getName(), annotationValueList );
		MockHelper.stringValue( "column", result.getColumn(), annotationValueList );
		return create( FIELD_RESULT, null, annotationValueList );
	}


	private void nestedFieldResultList(String name, List<JaxbSqlResultSetMappingFieldResult> fieldResultList, List<AnnotationValue> annotationValueList) {
		if ( CollectionHelper.isNotEmpty( fieldResultList ) ) {
			AnnotationValue[] values = new AnnotationValue[fieldResultList.size()];
			for ( int i = 0; i < fieldResultList.size(); i++ ) {
				AnnotationInstance annotationInstance = parseFieldResult( fieldResultList.get( i ) );
				values[i] = MockHelper.nestedAnnotationValue(
						"", annotationInstance
				);
			}
			MockHelper.addToCollectionIfNotNull(
					annotationValueList, AnnotationValue.createArrayValue( name, values )
			);
		}
	}

	private void parseNamedQueries( Collection<JaxbNamedQuery> namedQueries ) {
		if (! namedQueries.isEmpty() ) {
			AnnotationValue[] namedQueryAnnotations = new AnnotationValue[namedQueries.size()];
			int i = 0;
			for ( JaxbNamedQuery namedQuery : namedQueries ) {
				List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
				MockHelper.stringValue( "name", namedQuery.getName(), annotationValueList );
				MockHelper.stringValue( "query", namedQuery.getQuery(), annotationValueList );
				MockHelper.stringValue( "cacheRegion", namedQuery.getCacheRegion(), annotationValueList );
				MockHelper.stringValue( "comment", namedQuery.getComment(), annotationValueList );
				MockHelper.booleanValue( "cacheable", namedQuery.isCacheable(), annotationValueList );
				MockHelper.booleanValue( "readOnly", namedQuery.isReadOnly(), annotationValueList );
				MockHelper.integerValue( "fetchSize", namedQuery.getFetchSize(), annotationValueList );
				MockHelper.integerValue( "timeout", namedQuery.getTimeout(), annotationValueList );
				MockHelper.enumValue( "cacheMode", CACHE_MODE_TYPE, MockHelper.convert( namedQuery.getCacheMode() ),
						annotationValueList );
				MockHelper.enumValue( "flushMode", FLUSH_MODE_TYPE, MockHelper.convert( namedQuery.getFlushMode() ),
						annotationValueList );
				
				AnnotationInstance annotationInstance = create(
						HibernateDotNames.NAMED_QUERY, null, annotationValueList );
				namedQueryAnnotations[i++] = MockHelper.nestedAnnotationValue( "", annotationInstance );
			}
			
			List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
			MockHelper.addToCollectionIfNotNull( annotationValueList,
					AnnotationValue.createArrayValue( "value", namedQueryAnnotations ) );
			
			create( HibernateDotNames.NAMED_QUERIES, null, annotationValueList );
		}
	}
	
	private void parseNamedNativeQueries( Collection<JaxbNamedNativeQuery> namedQueries ) {
		if (! namedQueries.isEmpty() ) {
			AnnotationValue[] namedQueryAnnotations = new AnnotationValue[namedQueries.size()];
			int i = 0;
			for ( JaxbNamedNativeQuery namedQuery : namedQueries ) {
				List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
				MockHelper.stringValue( "name", namedQuery.getName(), annotationValueList );
				MockHelper.stringValue( "query", namedQuery.getQuery(), annotationValueList );
				MockHelper.stringValue( "cacheRegion", namedQuery.getCacheRegion(), annotationValueList );
				MockHelper.stringValue( "comment", namedQuery.getComment(), annotationValueList );
				MockHelper.stringValue( "resultSetMapping", namedQuery.getResultSetMapping(), annotationValueList );
				MockHelper.booleanValue( "cacheable", namedQuery.isCacheable(), annotationValueList );
				MockHelper.booleanValue( "readOnly", namedQuery.isReadOnly(), annotationValueList );
				// TODO: add #callable to the schema?
//				MockHelper.booleanValue( "callable", namedQuery.isCallable(), annotationValueList );
				MockHelper.integerValue( "fetchSize", namedQuery.getFetchSize(), annotationValueList );
				MockHelper.integerValue( "timeout", namedQuery.getTimeout(), annotationValueList );
				MockHelper.enumValue( "cacheMode", CACHE_MODE_TYPE,
						MockHelper.convert( namedQuery.getCacheMode() ), annotationValueList );
				MockHelper.enumValue( "flushMode", FLUSH_MODE_TYPE,
						MockHelper.convert( namedQuery.getFlushMode() ), annotationValueList );
				MockHelper.classValue( "resultClass", namedQuery.getResultClass(), annotationValueList, getDefaults(),
						indexBuilder.getServiceRegistry() );
				
				AnnotationInstance annotationInstance = create(
						HibernateDotNames.NAMED_NATIVE_QUERY, null, annotationValueList );
				namedQueryAnnotations[i++] = MockHelper.nestedAnnotationValue( "", annotationInstance );
			}
			
			List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
			MockHelper.addToCollectionIfNotNull( annotationValueList,
					AnnotationValue.createArrayValue( "value", namedQueryAnnotations ) );
			
			create( HibernateDotNames.NAMED_NATIVE_QUERIES, null, annotationValueList );
		}
	}

	//@QueryHint
	private AnnotationInstance parseQueryHint(JaxbQueryHint queryHint) {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", queryHint.getName(), annotationValueList );
		MockHelper.stringValue( "value", queryHint.getValue(), annotationValueList );
		return create( QUERY_HINT, null, annotationValueList );

	}

	private void nestedQueryHintList(String name, List<JaxbQueryHint> constraints, List<AnnotationValue> annotationValueList) {
		if ( CollectionHelper.isNotEmpty( constraints ) ) {
			AnnotationValue[] values = new AnnotationValue[constraints.size()];
			for ( int i = 0; i < constraints.size(); i++ ) {
				AnnotationInstance annotationInstance = parseQueryHint( constraints.get( i ) );
				values[i] = MockHelper.nestedAnnotationValue(
						"", annotationInstance
				);
			}
			MockHelper.addToCollectionIfNotNull(
					annotationValueList, AnnotationValue.createArrayValue( name, values )
			);
		}
	}


	//@SequenceGenerator
	private AnnotationInstance parseSequenceGenerator(JaxbSequenceGenerator generator) {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", generator.getName(), annotationValueList );
		MockHelper.stringValue( "catalog", generator.getCatalog(), annotationValueList );
		MockHelper.stringValue( "schema", generator.getSchema(), annotationValueList );
		MockHelper.stringValue( "sequenceName", generator.getSequenceName(), annotationValueList );
		MockHelper.integerValue( "initialValue", generator.getInitialValue(), annotationValueList );
		MockHelper.integerValue( "allocationSize", generator.getAllocationSize(), annotationValueList );
		return
				create(
						SEQUENCE_GENERATOR, null, annotationValueList

				);
	}

	//@TableGenerator
	private AnnotationInstance parseTableGenerator(JaxbTableGenerator generator) {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", generator.getName(), annotationValueList );
		MockHelper.stringValue( "catalog", generator.getCatalog(), annotationValueList );
		MockHelper.stringValue( "schema", generator.getSchema(), annotationValueList );
		MockHelper.stringValue( "table", generator.getTable(), annotationValueList );
		MockHelper.stringValue( "pkColumnName", generator.getPkColumnName(), annotationValueList );
		MockHelper.stringValue( "valueColumnName", generator.getValueColumnName(), annotationValueList );
		MockHelper.stringValue( "pkColumnValue", generator.getPkColumnValue(), annotationValueList );
		MockHelper.integerValue( "initialValue", generator.getInitialValue(), annotationValueList );
		MockHelper.integerValue( "allocationSize", generator.getAllocationSize(), annotationValueList );
		nestedUniqueConstraintList( "uniqueConstraints", generator.getUniqueConstraint(), annotationValueList );
		nestedIndexConstraintList( "indexes", generator.getIndex(), annotationValueList );
		return
				create(
						TABLE_GENERATOR, null, annotationValueList

				);
	}
	
	private void parseFilterDefs(Collection<JaxbHbmFilterDef> filterDefs) {
		if (! filterDefs.isEmpty() ) {
			AnnotationValue[] filterDefAnnotations = new AnnotationValue[filterDefs.size()];
			int i = 0;
			for ( JaxbHbmFilterDef filterDef : filterDefs ) {
				List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
				MockHelper.stringValue( "name", filterDef.getName(), annotationValueList );
				MockHelper.stringValue( "defaultCondition", filterDef.getCondition(), annotationValueList );
				nestedFilterParams( filterDef.getFilterParam(), annotationValueList );
				
				AnnotationInstance annotationInstance = create( FILTER_DEF, null, annotationValueList );
				filterDefAnnotations[i++] = MockHelper.nestedAnnotationValue( "", annotationInstance );
			}
			
			List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
			MockHelper.addToCollectionIfNotNull( annotationValueList,
					AnnotationValue.createArrayValue( "value", filterDefAnnotations ) );
			
			create( FILTER_DEFS, null, annotationValueList );
		}
	}
	
	private void nestedFilterParams(List<JaxbFilterParam> filterParams, List<AnnotationValue> annotationValueList) {
		if (! filterParams.isEmpty() ) {
			AnnotationValue[] filterParamAnnotations = new AnnotationValue[filterParams.size()];
			int i = 0;
			for ( JaxbFilterParam filterParam : filterParams ) {
				List<AnnotationValue> filterParamannotationValueList = new ArrayList<AnnotationValue>();
				MockHelper.stringValue( "name", filterParam.getName(), filterParamannotationValueList );
				MockHelper.stringValue( "type", filterParam.getType(), filterParamannotationValueList );
				
				AnnotationInstance annotationInstance = create( PARAM_DEF, null, filterParamannotationValueList );
				filterParamAnnotations[i++] = MockHelper.nestedAnnotationValue( "", annotationInstance );
			}
			MockHelper.addToCollectionIfNotNull( annotationValueList,
					AnnotationValue.createArrayValue( "parameters", filterParamAnnotations ) );
		}
	}
	
	private void parseFetchProfiles(Collection<JaxbHbmFetchProfile> fetchProfiles) {
		if (! fetchProfiles.isEmpty() ) {
			AnnotationValue[] fetchProfileAnnotations = new AnnotationValue[fetchProfiles.size()];
			int i = 0;
			for ( JaxbHbmFetchProfile fetchProfile : fetchProfiles ) {
				AnnotationInstance annotationInstance = parseFetchProfile( fetchProfile );
				fetchProfileAnnotations[i++] = MockHelper.nestedAnnotationValue( "", annotationInstance );
			}
			
			List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
			MockHelper.addToCollectionIfNotNull( annotationValueList,
					AnnotationValue.createArrayValue( "value", fetchProfileAnnotations ) );
			
			create( FETCH_PROFILES, null, annotationValueList );
		}
	}
	
	private AnnotationInstance parseFetchProfile(JaxbHbmFetchProfile fetchProfile) {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", fetchProfile.getName(), annotationValueList );
		
		AnnotationValue[] fetchAnnotations = new AnnotationValue[fetchProfile.getFetch().size()];
		int i = 0;
		for ( JaxbFetch fetch : fetchProfile.getFetch() ) {
			List<AnnotationValue> fetchAnnotationValueList = new ArrayList<AnnotationValue>();
			MockHelper.stringValue( "association", fetch.getAssociation(), fetchAnnotationValueList );
			MockHelper.classValue( "entity", fetch.getEntity(), fetchAnnotationValueList, getDefaults(),
					indexBuilder.getServiceRegistry() );
			MockHelper.enumValue( "mode", FETCH_MODE, convertFetchMode( fetch.getStyle() ),
					fetchAnnotationValueList );
			AnnotationInstance annotationInstance = create( FETCH_OVERRIDE, null, fetchAnnotationValueList );
			fetchAnnotations[i++] = MockHelper.nestedAnnotationValue( "", annotationInstance );
		}
		
		MockHelper.addToCollectionIfNotNull( annotationValueList,
				AnnotationValue.createArrayValue( "fetchOverrides", fetchAnnotations ) );
		
		return create(FETCH_PROFILE, null, annotationValueList );
	}
	
	private FetchMode convertFetchMode(String fetchMode) {
		if (fetchMode.equalsIgnoreCase( "join" )) {
			return FetchMode.JOIN;
		}
		else if (fetchMode.equalsIgnoreCase( "subselect" )) {
			return FetchMode.SUBSELECT;
		}
		else {
			return FetchMode.SELECT;
		}
	}

	@Override
	protected AnnotationInstance push(AnnotationInstance annotationInstance) {
		if ( annotationInstance != null ) {
			return globalAnnotations.push( annotationInstance.name(), annotationInstance );
		}
		return null;
	}
}
