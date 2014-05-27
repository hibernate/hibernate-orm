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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.AccessType;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.jaxb.JaxbAttributes;
import org.hibernate.metamodel.source.internal.jaxb.JaxbCacheElement;
import org.hibernate.metamodel.source.internal.jaxb.JaxbDiscriminatorColumn;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntity;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntityListeners;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmFilter;
import org.hibernate.metamodel.source.internal.jaxb.JaxbIdClass;
import org.hibernate.metamodel.source.internal.jaxb.JaxbInheritance;
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

	public EntityMocker(IndexBuilder indexBuilder, JaxbEntity entity, Default defaults) {
		super( indexBuilder, defaults );
		this.entity = entity;
	}

	@Override
	protected void doProcess() {
		//@Entity
		create( ENTITY, MockHelper.stringValueArray( "name", entity.getName() ) );

		if ( entity.isCacheable() != null ) {
			//@Cacheable
			create( CACHEABLE, MockHelper.booleanValueArray( "value", entity.isCacheable() ) );
		}
		if ( StringHelper.isNotEmpty( entity.getDiscriminatorValue() ) ) {
			//@DiscriminatorValue
			create( DISCRIMINATOR_VALUE, MockHelper.stringValueArray( "value", entity.getDiscriminatorValue() ) );
		}
		if (entity.isSelectBeforeUpdate() != null) {
			create( SELECT_BEFORE_UPDATE, MockHelper.booleanValueArray( "value", entity.isSelectBeforeUpdate() ) );
		}
		
		//@Table
		parseTable( entity.getTable() );
		parseInheritance( entity.getInheritance() );
		parseDiscriminatorColumn( entity.getDiscriminatorColumn() );
		parseAttributeOverrides( entity.getAttributeOverride(), getTarget() );
		parseAssociationOverrides( entity.getAssociationOverride(), getTarget() );
		parsePrimaryKeyJoinColumnList( entity.getPrimaryKeyJoinColumn(), getTarget() );
		parseSecondaryTableList( entity.getSecondaryTable(), getTarget() );
		
		//@Cache
		parseCache( entity.getCache(), getTarget() );
		// @Filters
		parseFilters( entity.getFilter(), getTarget() );
		// @BatchSize
		parseBatchSize( entity.getBatchSize(), getTarget() );
		// @Proxy
		parseProxy( entity.getProxy(), entity.isLazy(), getTarget() );
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
		return create( JPADotNames.TABLE, annotationValueList );
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

	protected void parseSecondaryTableList(List<JaxbSecondaryTable> primaryKeyJoinColumnList, AnnotationTarget target) {
		if ( CollectionHelper.isNotEmpty( primaryKeyJoinColumnList ) ) {
			if ( primaryKeyJoinColumnList.size() == 1 ) {
				parseSecondaryTable( primaryKeyJoinColumnList.get( 0 ), target );
				parseHibernateTable( primaryKeyJoinColumnList.get( 0 ), target );
			}
			else {
				create(
						SECONDARY_TABLES,
						target,
						nestedSecondaryTableList( "value", primaryKeyJoinColumnList, null )
				);
				create(
						TABLES,
						target,
						nestedHibernateTableList( "value", primaryKeyJoinColumnList, null )
				);
			}
		}
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

	protected AnnotationValue[] nestedHibernateTableList(String name, List<JaxbSecondaryTable> secondaryTableList, List<AnnotationValue> annotationValueList) {
		if ( CollectionHelper.isNotEmpty( secondaryTableList ) ) {
			AnnotationValue[] values = new AnnotationValue[secondaryTableList.size()];
			for ( int i = 0; i < secondaryTableList.size(); i++ ) {
				AnnotationInstance annotationInstance = parseHibernateTable( secondaryTableList.get( i ), null );
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

	//Hibernate @Table
	protected AnnotationInstance parseHibernateTable(JaxbSecondaryTable secondaryTable, AnnotationTarget target) {
		if ( secondaryTable == null ) {
			return null;
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "appliesTo", secondaryTable.getName(), annotationValueList );
		MockHelper.booleanValue( "optional", secondaryTable.isOptional(), annotationValueList );
		return create( HibernateDotNames.TABLE, target, annotationValueList );
	}
	
	private void parseCache(JaxbCacheElement cache, AnnotationTarget target) {
		if ( cache == null ) {
			return;
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "region", cache.getRegion(), annotationValueList );
		MockHelper.stringValue( "include", cache.getInclude(), annotationValueList );
		MockHelper.enumValue( "usage", CACHE_CONCURRENCY_STRATEGY,
				CacheConcurrencyStrategy.parse( cache.getUsage() ), annotationValueList );
		create( CACHE, target, annotationValueList );
	}
	
	private void parseFilters(List<JaxbHbmFilter> filters, AnnotationTarget target) {
		if (! filters.isEmpty() ) {
			AnnotationValue[] filterAnnotations = new AnnotationValue[filters.size()];
			int i = 0;
			for ( JaxbHbmFilter filter : filters ) {
				List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
				MockHelper.stringValue( "name", filter.getName(), annotationValueList );
				
				String condition = "";
				if (! StringHelper.isEmpty( filter.getConditionAttribute() )) {
					condition = filter.getConditionAttribute();
				}
				
				for (Serializable contentElement : filter.getContent()) {
					if ( String.class.isInstance( contentElement ) ) {
						String s = (String) contentElement;
						condition = s.trim();
					}
					// TODO: Could be aliases -- see xsd
				}
				
				MockHelper.stringValue( "condition", condition, annotationValueList );
				
				AnnotationInstance annotationInstance = create( FILTER, null, annotationValueList );
				filterAnnotations[i++] = MockHelper.nestedAnnotationValue( "", annotationInstance );
			}
			
			List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
			MockHelper.addToCollectionIfNotNull( annotationValueList,
					AnnotationValue.createArrayValue( "value", filterAnnotations ) );
			
			create( FILTERS, target, annotationValueList );
		}
	}
	
	private void parseBatchSize(Integer batchSize, AnnotationTarget target) {
		if ( batchSize == null ) {
			return;
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.integerValue( "size", batchSize, annotationValueList );
		create( BATCH_SIZE, target, annotationValueList );
	}
	
	private void parseProxy(String proxy, Boolean isLazy, AnnotationTarget target) {
		if ( StringHelper.isEmpty( proxy ) ) {
			return;
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.classValue( "proxyClass", proxy, annotationValueList, getDefaults(),
				indexBuilder.getServiceRegistry() );
		if (isLazy != null) {
			MockHelper.booleanValue( "lazy", isLazy, annotationValueList );
		}
		create( PROXY, target, annotationValueList );
	}
}
