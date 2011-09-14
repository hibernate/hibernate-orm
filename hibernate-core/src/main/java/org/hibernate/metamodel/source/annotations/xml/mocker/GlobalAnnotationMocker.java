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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import org.hibernate.internal.jaxb.mapping.orm.JaxbColumnResult;
import org.hibernate.internal.jaxb.mapping.orm.JaxbEntityResult;
import org.hibernate.internal.jaxb.mapping.orm.JaxbFieldResult;
import org.hibernate.internal.jaxb.mapping.orm.JaxbNamedNativeQuery;
import org.hibernate.internal.jaxb.mapping.orm.JaxbNamedQuery;
import org.hibernate.internal.jaxb.mapping.orm.JaxbQueryHint;
import org.hibernate.internal.jaxb.mapping.orm.JaxbSequenceGenerator;
import org.hibernate.internal.jaxb.mapping.orm.JaxbSqlResultSetMapping;
import org.hibernate.internal.jaxb.mapping.orm.JaxbTableGenerator;

/**
 * @author Strong Liu
 */
class GlobalAnnotationMocker extends AbstractMocker {
	private GlobalAnnotations globalAnnotations;

	GlobalAnnotationMocker(IndexBuilder indexBuilder, GlobalAnnotations globalAnnotations) {
		super( indexBuilder );
		this.globalAnnotations = globalAnnotations;
	}


	void process() {
		if ( !globalAnnotations.getTableGeneratorMap().isEmpty() ) {
			for ( JaxbTableGenerator generator : globalAnnotations.getTableGeneratorMap().values() ) {
				parserTableGenerator( generator );
			}
		}
		if ( !globalAnnotations.getSequenceGeneratorMap().isEmpty() ) {
			for ( JaxbSequenceGenerator generator : globalAnnotations.getSequenceGeneratorMap().values() ) {
				parserSequenceGenerator( generator );
			}
		}
		if ( !globalAnnotations.getNamedQueryMap().isEmpty() ) {
			Collection<JaxbNamedQuery> namedQueries = globalAnnotations.getNamedQueryMap().values();
			if ( namedQueries.size() > 1 ) {
				parserNamedQueries( namedQueries );
			}
			else {
				parserNamedQuery( namedQueries.iterator().next() );
			}
		}
		if ( !globalAnnotations.getNamedNativeQueryMap().isEmpty() ) {
			Collection<JaxbNamedNativeQuery> namedQueries = globalAnnotations.getNamedNativeQueryMap().values();
			if ( namedQueries.size() > 1 ) {
				parserNamedNativeQueries( namedQueries );
			}
			else {
				parserNamedNativeQuery( namedQueries.iterator().next() );
			}
		}
		if ( !globalAnnotations.getSqlResultSetMappingMap().isEmpty() ) {
			parserSqlResultSetMappings( globalAnnotations.getSqlResultSetMappingMap().values() );
		}
		indexBuilder.finishGlobalConfigurationMocking( globalAnnotations );
	}

	private AnnotationInstance parserSqlResultSetMappings(Collection<JaxbSqlResultSetMapping> namedQueries) {
		AnnotationValue[] values = new AnnotationValue[namedQueries.size()];
		int i = 0;
		for ( Iterator<JaxbSqlResultSetMapping> iterator = namedQueries.iterator(); iterator.hasNext(); ) {
			AnnotationInstance annotationInstance = parserSqlResultSetMapping( iterator.next() );
			values[i++] = MockHelper.nestedAnnotationValue(
					"", annotationInstance
			);
		}
		return create(
				SQL_RESULT_SET_MAPPINGS, null,
				new AnnotationValue[] { AnnotationValue.createArrayValue( "values", values ) }

		);
	}


	//@SqlResultSetMapping
	private AnnotationInstance parserSqlResultSetMapping(JaxbSqlResultSetMapping mapping) {

		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", mapping.getName(), annotationValueList );
		nestedEntityResultList( "entities", mapping.getEntityResult(), annotationValueList );
		nestedColumnResultList( "columns", mapping.getColumnResult(), annotationValueList );
		return
				create(
						SQL_RESULT_SET_MAPPING, null, annotationValueList

				);
	}


	//@EntityResult
	private AnnotationInstance parserEntityResult(JaxbEntityResult result) {

		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue(
				"discriminatorColumn", result.getDiscriminatorColumn(), annotationValueList
		);
		nestedFieldResultList( "fields", result.getFieldResult(), annotationValueList );
		MockHelper.classValue(
				"entityClass", result.getEntityClass(), annotationValueList, indexBuilder.getServiceRegistry()
		);
		return
				create(
						ENTITY_RESULT, null, annotationValueList

				);
	}

	private void nestedEntityResultList(String name, List<JaxbEntityResult> entityResults, List<AnnotationValue> annotationValueList) {
		if ( MockHelper.isNotEmpty( entityResults ) ) {
			AnnotationValue[] values = new AnnotationValue[entityResults.size()];
			for ( int i = 0; i < entityResults.size(); i++ ) {
				AnnotationInstance annotationInstance = parserEntityResult( entityResults.get( i ) );
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
	private AnnotationInstance parserColumnResult(JaxbColumnResult result) {
		return create( COLUMN_RESULT, null, MockHelper.stringValueArray( "name", result.getName() ) );
	}

	private void nestedColumnResultList(String name, List<JaxbColumnResult> columnResults, List<AnnotationValue> annotationValueList) {
		if ( MockHelper.isNotEmpty( columnResults ) ) {
			AnnotationValue[] values = new AnnotationValue[columnResults.size()];
			for ( int i = 0; i < columnResults.size(); i++ ) {
				AnnotationInstance annotationInstance = parserColumnResult( columnResults.get( i ) );
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
	private AnnotationInstance parserFieldResult(JaxbFieldResult result) {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", result.getName(), annotationValueList );
		MockHelper.stringValue( "column", result.getColumn(), annotationValueList );
		return create( FIELD_RESULT, null, annotationValueList );
	}


	private void nestedFieldResultList(String name, List<JaxbFieldResult> fieldResultList, List<AnnotationValue> annotationValueList) {
		if ( MockHelper.isNotEmpty( fieldResultList ) ) {
			AnnotationValue[] values = new AnnotationValue[fieldResultList.size()];
			for ( int i = 0; i < fieldResultList.size(); i++ ) {
				AnnotationInstance annotationInstance = parserFieldResult( fieldResultList.get( i ) );
				values[i] = MockHelper.nestedAnnotationValue(
						"", annotationInstance
				);
			}
			MockHelper.addToCollectionIfNotNull(
					annotationValueList, AnnotationValue.createArrayValue( name, values )
			);
		}
	}

	private AnnotationInstance parserNamedNativeQueries(Collection<JaxbNamedNativeQuery> namedQueries) {
		AnnotationValue[] values = new AnnotationValue[namedQueries.size()];
		int i = 0;
		for ( Iterator<JaxbNamedNativeQuery> iterator = namedQueries.iterator(); iterator.hasNext(); ) {
			AnnotationInstance annotationInstance = parserNamedNativeQuery( iterator.next() );
			values[i++] = MockHelper.nestedAnnotationValue(
					"", annotationInstance
			);
		}
		return create(
				NAMED_NATIVE_QUERIES, null,
				new AnnotationValue[] { AnnotationValue.createArrayValue( "values", values ) }

		);
	}

	//@NamedNativeQuery
	private AnnotationInstance parserNamedNativeQuery(JaxbNamedNativeQuery namedNativeQuery) {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", namedNativeQuery.getName(), annotationValueList );
		MockHelper.stringValue( "query", namedNativeQuery.getQuery(), annotationValueList );
		MockHelper.stringValue(
				"resultSetMapping", namedNativeQuery.getResultSetMapping(), annotationValueList
		);
		MockHelper.classValue(
				"resultClass", namedNativeQuery.getResultClass(), annotationValueList, indexBuilder.getServiceRegistry()
		);
		nestedQueryHintList( "hints", namedNativeQuery.getHint(), annotationValueList );
		return
				create(
						NAMED_NATIVE_QUERY, null, annotationValueList

				);
	}


	private AnnotationInstance parserNamedQueries(Collection<JaxbNamedQuery> namedQueries) {
		AnnotationValue[] values = new AnnotationValue[namedQueries.size()];
		int i = 0;
		for ( Iterator<JaxbNamedQuery> iterator = namedQueries.iterator(); iterator.hasNext(); ) {
			AnnotationInstance annotationInstance = parserNamedQuery( iterator.next() );
			values[i++] = MockHelper.nestedAnnotationValue(
					"", annotationInstance
			);
		}
		return create(
				NAMED_QUERIES, null,
				new AnnotationValue[] { AnnotationValue.createArrayValue( "values", values ) }

		);
	}


	//@NamedQuery
	private AnnotationInstance parserNamedQuery(JaxbNamedQuery namedQuery) {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", namedQuery.getName(), annotationValueList );
		MockHelper.stringValue( "query", namedQuery.getQuery(), annotationValueList );
		MockHelper.enumValue( "lockMode", LOCK_MODE_TYPE, namedQuery.getLockMode(), annotationValueList );
		nestedQueryHintList( "hints", namedQuery.getHint(), annotationValueList );
		return create( NAMED_QUERY, null, annotationValueList );
	}

	//@QueryHint
	private AnnotationInstance parserQueryHint(JaxbQueryHint queryHint) {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", queryHint.getName(), annotationValueList );
		MockHelper.stringValue( "value", queryHint.getValue(), annotationValueList );
		return create( QUERY_HINT, null, annotationValueList );

	}

	private void nestedQueryHintList(String name, List<JaxbQueryHint> constraints, List<AnnotationValue> annotationValueList) {
		if ( MockHelper.isNotEmpty( constraints ) ) {
			AnnotationValue[] values = new AnnotationValue[constraints.size()];
			for ( int i = 0; i < constraints.size(); i++ ) {
				AnnotationInstance annotationInstance = parserQueryHint( constraints.get( i ) );
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
	private AnnotationInstance parserSequenceGenerator(JaxbSequenceGenerator generator) {
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
	private AnnotationInstance parserTableGenerator(JaxbTableGenerator generator) {
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
		return
				create(
						TABLE_GENERATOR, null, annotationValueList

				);
	}

	@Override
	protected AnnotationInstance push(AnnotationInstance annotationInstance) {
		if ( annotationInstance != null ) {
			return globalAnnotations.push( annotationInstance.name(), annotationInstance );
		}
		return null;
	}
}
