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
import java.util.List;
import java.util.Map;

import javax.persistence.AccessType;

import org.hibernate.annotations.CacheModeType;
import org.hibernate.annotations.FlushModeType;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.jaxb.JaxbAttributes;
import org.hibernate.metamodel.source.internal.jaxb.JaxbDiscriminatorColumn;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntity;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntityListeners;
import org.hibernate.metamodel.source.internal.jaxb.JaxbIdClass;
import org.hibernate.metamodel.source.internal.jaxb.JaxbInheritance;
import org.hibernate.metamodel.source.internal.jaxb.JaxbNamedNativeQuery;
import org.hibernate.metamodel.source.internal.jaxb.JaxbNamedQuery;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPostLoad;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPostPersist;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPostRemove;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPostUpdate;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPrePersist;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPreRemove;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPreUpdate;
import org.hibernate.metamodel.source.internal.jaxb.JaxbSecondaryTable;
import org.hibernate.metamodel.source.internal.jaxb.JaxbTable;
import org.hibernate.metamodel.source.internal.jaxb.ManagedType;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

/**
 * Mock <entity> to {@link javax.persistence.Entity @Entity}
 *
 * @author Strong Liu
 * @author Brett Meyer
 */
public class EntityMocker extends AbstractEntityObjectMocker {
	private final JaxbEntity entity;

	public EntityMocker(IndexBuilder indexBuilder, JaxbEntity entity, EntityMappingsMocker.Default defaults) {
		super( indexBuilder, defaults );
		this.entity = entity;
	}

	@Override
	protected void processExtra() {
		//@Entity
		create( ENTITY, MockHelper.stringValueArray( "name", entity.getName() ) );


		if ( entity.isCacheable() != null ) {
			//@Cacheable
			create(
					CACHEABLE,
					MockHelper.booleanValueArray( "value", entity.isCacheable() )

			);
		}
		if ( StringHelper.isNotEmpty( entity.getDiscriminatorValue() ) ) {
			//@DiscriminatorValue
			create(
					DISCRIMINATOR_VALUE,
					MockHelper.stringValueArray( "value", entity.getDiscriminatorValue() )

			);
		}
		//@Table
		parseTable( entity.getTable() );
		parseInheritance( entity.getInheritance() );
		parseDiscriminatorColumn( entity.getDiscriminatorColumn() );
		parseAttributeOverrides( entity.getAttributeOverride(), getTarget() );
		parseAssociationOverrides( entity.getAssociationOverride(), getTarget() );
		parsePrimaryKeyJoinColumnList( entity.getPrimaryKeyJoinColumn(), getTarget() );
		parseSecondaryTableList( entity.getSecondaryTable(), getTarget() );

		// @NamedQuery
		parseNamedQueries( entity.getNamedQuery() );
		// @NamedNativeQuery
		parseNamedNativeQueries( entity.getNamedNativeQuery() );
	}

	//@Table  (entity only)
	private AnnotationInstance parseTable(JaxbTable table) {
		if ( table == null ) {
			return null;
		}
		DefaultConfigurationHelper.INSTANCE.applyDefaults(table,getDefaults());
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", table.getName(), annotationValueList );
		MockHelper.stringValue( "catalog", table.getCatalog(), annotationValueList );
		MockHelper.stringValue( "schema", table.getSchema(), annotationValueList );
		nestedUniqueConstraintList( "uniqueConstraints", table.getUniqueConstraint(), annotationValueList );
		nestedIndexConstraintList( "indexes", table.getIndex(), annotationValueList );
		return create( TABLE, annotationValueList );
	}

	protected AccessType getDefaultAccess() {
		if ( entity.getAccess() != null ) {
			return entity.getAccess();
		}

		return null;
	}

	protected AccessType getAccessFromIndex(DotName className) {
		Map<DotName, List<AnnotationInstance>> indexedAnnotations = indexBuilder.getIndexedAnnotations( className );
		List<AnnotationInstance> accessAnnotationInstances = indexedAnnotations.get( ACCESS );
		if ( CollectionHelper.isNotEmpty( accessAnnotationInstances ) ) {
			for ( AnnotationInstance annotationInstance : accessAnnotationInstances ) {
				if ( annotationInstance.target() != null && annotationInstance.target() instanceof ClassInfo ) {
					ClassInfo ci = (ClassInfo) ( annotationInstance.target() );
					if ( className.equals( ci.name() ) ) {
						//todo does ci need to have @Entity or @MappedSuperClass ??
						return AccessType.valueOf( annotationInstance.value().asEnum() );
					}
				}
			}
		}
		return null;
	}

	@Override
	protected ManagedType getEntityElement() {
		return entity;
	}

	@Override
	protected JaxbPrePersist getPrePersist() {
		return entity.getPrePersist();
	}

	@Override
	protected JaxbPreRemove getPreRemove() {
		return entity.getPreRemove();
	}

	@Override
	protected JaxbPreUpdate getPreUpdate() {
		return entity.getPreUpdate();
	}

	@Override
	protected JaxbPostPersist getPostPersist() {
		return entity.getPostPersist();
	}

	@Override
	protected JaxbPostUpdate getPostUpdate() {
		return entity.getPostUpdate();
	}

	@Override
	protected JaxbPostRemove getPostRemove() {
		return entity.getPostRemove();
	}

	@Override
	protected JaxbPostLoad getPostLoad() {
		return entity.getPostLoad();
	}

	@Override
	protected JaxbAttributes getAttributes() {
		return entity.getAttributes();
	}


	@Override
	protected boolean isExcludeDefaultListeners() {
		return entity.getExcludeDefaultListeners() != null;
	}

	@Override
	protected boolean isExcludeSuperclassListeners() {
		return entity.getExcludeSuperclassListeners() != null;
	}

	@Override
	protected JaxbIdClass getIdClass() {
		return entity.getIdClass();
	}

	@Override
	protected JaxbEntityListeners getEntityListeners() {
		return entity.getEntityListeners();
	}

	//@Inheritance
	protected AnnotationInstance parseInheritance(JaxbInheritance inheritance) {
		if ( inheritance == null ) {
			return null;
		}
		return
				create(
						INHERITANCE,
						MockHelper.enumValueArray( "strategy", INHERITANCE_TYPE, inheritance.getStrategy() )

				);
	}

	//@DiscriminatorColumn
	protected AnnotationInstance parseDiscriminatorColumn(JaxbDiscriminatorColumn discriminatorColumn) {
		if ( discriminatorColumn == null ) {
			return null;
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", discriminatorColumn.getName(), annotationValueList );
		MockHelper.stringValue(
				"columnDefinition", discriminatorColumn.getColumnDefinition(), annotationValueList
		);
		MockHelper.integerValue( "length", discriminatorColumn.getLength(), annotationValueList );
		MockHelper.enumValue(
				"discriminatorType", DISCRIMINATOR_TYPE, discriminatorColumn.getDiscriminatorType(), annotationValueList
		);
		return create(DISCRIMINATOR_COLUMN, annotationValueList);

	}

	//@SecondaryTable
	protected AnnotationInstance parseSecondaryTable(JaxbSecondaryTable secondaryTable, AnnotationTarget target) {
		if ( secondaryTable == null ) {
			return null;
		}
		DefaultConfigurationHelper.INSTANCE.applyDefaults(secondaryTable,getDefaults());
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", secondaryTable.getName(), annotationValueList );
		MockHelper.stringValue( "catalog", secondaryTable.getCatalog(), annotationValueList );
		MockHelper.stringValue( "schema", secondaryTable.getSchema(), annotationValueList );
		nestedPrimaryKeyJoinColumnList("pkJoinColumns", secondaryTable.getPrimaryKeyJoinColumn(), annotationValueList);
		nestedUniqueConstraintList("uniqueConstraints", secondaryTable.getUniqueConstraint(), annotationValueList);
		nestedIndexConstraintList( "indexes", secondaryTable.getIndex(), annotationValueList );
		return create( SECONDARY_TABLE, target, annotationValueList );
	}


	protected AnnotationInstance parseSecondaryTableList(List<JaxbSecondaryTable> primaryKeyJoinColumnList, AnnotationTarget target) {
		if ( CollectionHelper.isNotEmpty( primaryKeyJoinColumnList ) ) {
			if ( primaryKeyJoinColumnList.size() == 1 ) {
				return parseSecondaryTable( primaryKeyJoinColumnList.get( 0 ), target );
			}
			else {
				return create(
						SECONDARY_TABLES,
						target,
						nestedSecondaryTableList( "value", primaryKeyJoinColumnList, null )
				);
			}
		}
		return null;

	}

	protected AnnotationValue[] nestedSecondaryTableList(String name, List<JaxbSecondaryTable> secondaryTableList, List<AnnotationValue> annotationValueList) {
		if ( CollectionHelper.isNotEmpty( secondaryTableList ) ) {
			AnnotationValue[] values = new AnnotationValue[secondaryTableList.size()];
			for ( int i = 0; i < secondaryTableList.size(); i++ ) {
				AnnotationInstance annotationInstance = parseSecondaryTable( secondaryTableList.get( i ), null );
				values[i] = MockHelper.nestedAnnotationValue(
						"", annotationInstance
				);
			}
			MockHelper.addToCollectionIfNotNull(
					annotationValueList, AnnotationValue.createArrayValue( name, values )
			);
			return values;
		}
		return MockHelper.EMPTY_ANNOTATION_VALUE_ARRAY;

	}
	
	private void parseNamedQueries( List<JaxbNamedQuery> namedQueries ) {
		if (! namedQueries.isEmpty() ) {
			AnnotationValue[] namedQueryAnnotations = new AnnotationValue[namedQueries.size()];
			for ( int i = 0; i < namedQueries.size(); i++ ) {
				JaxbNamedQuery namedQuery = namedQueries.get( i );
				
				List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
				MockHelper.stringValue( "name", namedQuery.getName(), annotationValueList );
				MockHelper.stringValue( "query", namedQuery.getQuery(), annotationValueList );
				MockHelper.stringValue( "cacheRegion", namedQuery.getCacheRegion(), annotationValueList );
				MockHelper.stringValue( "comment", namedQuery.getComment(), annotationValueList );
				MockHelper.booleanValue( "cacheable", namedQuery.isCacheable(), annotationValueList );
				MockHelper.booleanValue( "readOnly", namedQuery.isReadOnly(), annotationValueList );
				MockHelper.integerValue( "fetchSize", namedQuery.getFetchSize(), annotationValueList );
				MockHelper.integerValue( "timeout", namedQuery.getTimeout(), annotationValueList );
				MockHelper.enumValue( "cacheMode", HibernateDotNames.CACHE_MODE_TYPE,
						MockHelper.convert( namedQuery.getCacheMode() ), annotationValueList );
				MockHelper.enumValue( "flushMode", HibernateDotNames.FLUSH_MODE_TYPE,
						MockHelper.convert( namedQuery.getFlushMode() ), annotationValueList );
				
				AnnotationInstance annotationInstance = create(
						HibernateDotNames.NAMED_QUERY, null, annotationValueList );
				namedQueryAnnotations[i] = MockHelper.nestedAnnotationValue( "", annotationInstance );
			}
			
			List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
			MockHelper.addToCollectionIfNotNull( annotationValueList,
					AnnotationValue.createArrayValue( "value", namedQueryAnnotations ) );
			
			create( HibernateDotNames.NAMED_QUERIES, getTarget(), annotationValueList );
		}
	}
	
	private void parseNamedNativeQueries( List<JaxbNamedNativeQuery> namedQueries ) {
		if (! namedQueries.isEmpty() ) {
			AnnotationValue[] namedQueryAnnotations = new AnnotationValue[namedQueries.size()];
			for ( int i = 0; i < namedQueries.size(); i++ ) {
				JaxbNamedNativeQuery namedQuery = namedQueries.get( i );
				
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
				MockHelper.enumValue( "cacheMode", HibernateDotNames.CACHE_MODE_TYPE,
						MockHelper.convert( namedQuery.getCacheMode() ), annotationValueList );
				MockHelper.enumValue( "flushMode", HibernateDotNames.FLUSH_MODE_TYPE,
						MockHelper.convert( namedQuery.getFlushMode() ), annotationValueList );
				MockHelper.classValue( "resultClass", namedQuery.getResultClass(), annotationValueList,
						indexBuilder.getServiceRegistry() );
				
				AnnotationInstance annotationInstance = create(
						HibernateDotNames.NAMED_NATIVE_QUERY, null, annotationValueList );
				namedQueryAnnotations[i] = MockHelper.nestedAnnotationValue( "", annotationInstance );
			}
			
			List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
			MockHelper.addToCollectionIfNotNull( annotationValueList,
					AnnotationValue.createArrayValue( "value", namedQueryAnnotations ) );
			
			create( HibernateDotNames.NAMED_NATIVE_QUERIES, getTarget(), annotationValueList );
		}
	}
}
