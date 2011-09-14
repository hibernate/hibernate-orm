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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.jaxb.mapping.orm.JaxbAssociationOverride;
import org.hibernate.internal.jaxb.mapping.orm.JaxbAttributeOverride;
import org.hibernate.internal.jaxb.mapping.orm.JaxbCollectionTable;
import org.hibernate.internal.jaxb.mapping.orm.JaxbColumn;
import org.hibernate.internal.jaxb.mapping.orm.JaxbEnumType;
import org.hibernate.internal.jaxb.mapping.orm.JaxbJoinColumn;
import org.hibernate.internal.jaxb.mapping.orm.JaxbJoinTable;
import org.hibernate.internal.jaxb.mapping.orm.JaxbLob;
import org.hibernate.internal.jaxb.mapping.orm.JaxbOrderColumn;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPrimaryKeyJoinColumn;
import org.hibernate.internal.jaxb.mapping.orm.JaxbTemporalType;

/**
 * @author Strong Liu
 */
abstract class AnnotationMocker extends AbstractMocker {
	private EntityMappingsMocker.Default defaults;

	AnnotationMocker(IndexBuilder indexBuilder, EntityMappingsMocker.Default defaults) {
		super( indexBuilder );
		this.defaults = defaults;
	}

	abstract void process();

	protected EntityMappingsMocker.Default getDefaults() {
		return defaults;
	}

	protected boolean isDefaultCascadePersist() {
		return defaults.isCascadePersist()!=null && defaults.isCascadePersist();
	}

	//@JoinTable
	protected AnnotationInstance parserJoinTable(JaxbJoinTable joinTable, AnnotationTarget target) {
		if ( joinTable == null ) {
			return null;
		}
		DefaultConfigurationHelper.INSTANCE.applyDefaults(
				new SchemaAware.JoinTableSchemaAware( joinTable ),
				getDefaults()
		);
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", joinTable.getName(), annotationValueList );
		MockHelper.stringValue( "catalog", joinTable.getCatalog(), annotationValueList );
		MockHelper.stringValue( "schema", joinTable.getSchema(), annotationValueList );
		nestedJoinColumnList( "joinColumns", joinTable.getJoinColumn(), annotationValueList );
		nestedJoinColumnList(
				"inverseJoinColumns", joinTable.getInverseJoinColumn(), annotationValueList
		);
		nestedUniqueConstraintList(
				"uniqueConstraints", joinTable.getUniqueConstraint(), annotationValueList
		);
		return create( JOIN_TABLE, target, annotationValueList );
	}

	//@AssociationOverride
	private AnnotationInstance parserAssociationOverride(JaxbAssociationOverride associationOverride, AnnotationTarget target) {
		if ( associationOverride == null ) {
			return null;
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", associationOverride.getName(), annotationValueList );
		if ( associationOverride instanceof JaxbAssociationOverrideProxy ) {
			JaxbAssociationOverrideProxy proxy = (JaxbAssociationOverrideProxy) associationOverride;
			MockHelper.addToCollectionIfNotNull( annotationValueList, proxy.getJoinColumnsAnnotationValue() );
			MockHelper.addToCollectionIfNotNull( annotationValueList, proxy.getJoinTableAnnotationValue() );
		}
		else {
			nestedJoinColumnList(
					"joinColumns", associationOverride.getJoinColumn(), annotationValueList
			);
			MockHelper.nestedAnnotationValue(
					"joinTable", parserJoinTable( associationOverride.getJoinTable(), null ), annotationValueList
			);
		}
		return create( ASSOCIATION_OVERRIDE, target, annotationValueList );
	}

	private AnnotationValue[] nestedJoinColumnList(String name, List<JaxbJoinColumn> columns, List<AnnotationValue> annotationValueList) {
		if ( MockHelper.isNotEmpty( columns ) ) {
			AnnotationValue[] values = new AnnotationValue[columns.size()];
			for ( int i = 0; i < columns.size(); i++ ) {
				AnnotationInstance annotationInstance = parserJoinColumn( columns.get( i ), null );
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

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	//@Column
	protected AnnotationInstance parserColumn(JaxbColumn column, AnnotationTarget target) {
		if ( column == null ) {
			return null;
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", column.getName(), annotationValueList );
		MockHelper.stringValue( "columnDefinition", column.getColumnDefinition(), annotationValueList );
		MockHelper.stringValue( "table", column.getTable(), annotationValueList );
		MockHelper.booleanValue( "unique", column.isUnique(), annotationValueList );
		MockHelper.booleanValue( "nullable", column.isNullable(), annotationValueList );
		MockHelper.booleanValue( "insertable", column.isInsertable(), annotationValueList );
		MockHelper.booleanValue( "updatable", column.isUpdatable(), annotationValueList );
		MockHelper.integerValue( "length", column.getLength(), annotationValueList );
		MockHelper.integerValue( "precision", column.getPrecision(), annotationValueList );
		MockHelper.integerValue( "scale", column.getScale(), annotationValueList );
		return create( COLUMN, target, annotationValueList );
	}

	//@AttributeOverride
	private AnnotationInstance parserAttributeOverride(JaxbAttributeOverride attributeOverride, AnnotationTarget target) {
		if ( attributeOverride == null ) {
			return null;
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", attributeOverride.getName(), annotationValueList );
		if ( attributeOverride instanceof JaxbAttributeOverrideProxy ) {
			JaxbAttributeOverrideProxy proxy = (JaxbAttributeOverrideProxy) attributeOverride;
			MockHelper.addToCollectionIfNotNull( annotationValueList, proxy.getColumnAnnotationValue() );
		}
		else {
			MockHelper.nestedAnnotationValue(
					"column", parserColumn( attributeOverride.getColumn(), null ), annotationValueList
			);
		}
		return
				create(
						ATTRIBUTE_OVERRIDE, target, annotationValueList

				);
	}


	protected AnnotationInstance parserOrderColumn(JaxbOrderColumn orderColumn, AnnotationTarget target) {
		if ( orderColumn == null ) {
			return null;
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", orderColumn.getName(), annotationValueList );
		MockHelper.stringValue( "columnDefinition", orderColumn.getColumnDefinition(), annotationValueList );
		MockHelper.booleanValue( "nullable", orderColumn.isNullable(), annotationValueList );
		MockHelper.booleanValue( "insertable", orderColumn.isInsertable(), annotationValueList );
		MockHelper.booleanValue( "updatable", orderColumn.isUpdatable(), annotationValueList );
		return create( ORDER_COLUMN, target, annotationValueList );
	}

	//@JoinColumn
	protected AnnotationInstance parserJoinColumn(JaxbJoinColumn column, AnnotationTarget target) {
		if ( column == null ) {
			return null;
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", column.getName(), annotationValueList );
		MockHelper.stringValue( "columnDefinition", column.getColumnDefinition(), annotationValueList );
		MockHelper.stringValue( "table", column.getTable(), annotationValueList );
		MockHelper.stringValue(
				"referencedColumnName", column.getReferencedColumnName(), annotationValueList
		);
		MockHelper.booleanValue( "unique", column.isUnique(), annotationValueList );
		MockHelper.booleanValue( "nullable", column.isNullable(), annotationValueList );
		MockHelper.booleanValue( "insertable", column.isInsertable(), annotationValueList );
		MockHelper.booleanValue( "updatable", column.isUpdatable(), annotationValueList );
		return create( JOIN_COLUMN, target, annotationValueList );
	}

	protected AnnotationInstance parserLob(JaxbLob lob, AnnotationTarget target) {
		if ( lob == null ) {
			return null;
		}
		return create( LOB, target );
	}

	protected AnnotationInstance parserTemporalType(JaxbTemporalType temporalType, AnnotationTarget target) {
		if ( temporalType == null ) {
			return null;
		}
		return create( TEMPORAL, target, MockHelper.enumValueArray( "value", TEMPORAL_TYPE, temporalType ) );
	}

	protected AnnotationInstance parserEnumType(JaxbEnumType enumerated, AnnotationTarget target) {
		if ( enumerated == null ) {
			return null;
		}
		return create( ENUMERATED, target, MockHelper.enumValueArray( "value", ENUM_TYPE, enumerated ) );
	}


	protected AnnotationInstance parserPrimaryKeyJoinColumn(JaxbPrimaryKeyJoinColumn primaryKeyJoinColumn, AnnotationTarget target) {
		if ( primaryKeyJoinColumn == null ) {
			return null;
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", primaryKeyJoinColumn.getName(), annotationValueList );
		MockHelper.stringValue(
				"referencedColumnName", primaryKeyJoinColumn.getReferencedColumnName(), annotationValueList
		);
		MockHelper.stringValue(
				"columnDefinition", primaryKeyJoinColumn.getColumnDefinition(), annotationValueList
		);
		return
				create(
						PRIMARY_KEY_JOIN_COLUMN, target, annotationValueList

				);
	}

	protected AnnotationInstance parserPrimaryKeyJoinColumnList(List<JaxbPrimaryKeyJoinColumn> primaryKeyJoinColumnList, AnnotationTarget target) {
		if ( MockHelper.isNotEmpty( primaryKeyJoinColumnList ) ) {
			if ( primaryKeyJoinColumnList.size() == 1 ) {
				return parserPrimaryKeyJoinColumn( primaryKeyJoinColumnList.get( 0 ), target );
			}
			else {
				return create(
						PRIMARY_KEY_JOIN_COLUMNS,
						target,
						nestedPrimaryKeyJoinColumnList( "value", primaryKeyJoinColumnList, null )
				);
			}
		}

		return null;

	}

	protected AnnotationValue[] nestedPrimaryKeyJoinColumnList(String name, List<JaxbPrimaryKeyJoinColumn> constraints, List<AnnotationValue> annotationValueList) {
		if ( MockHelper.isNotEmpty( constraints ) ) {
			AnnotationValue[] values = new AnnotationValue[constraints.size()];
			for ( int i = 0; i < constraints.size(); i++ ) {
				AnnotationInstance annotationInstance = parserPrimaryKeyJoinColumn( constraints.get( i ), null );
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

	protected void getAnnotationInstanceByTarget(DotName annName, AnnotationTarget target, Operation operation) {
		Map<DotName, List<AnnotationInstance>> annotatedMap = indexBuilder.getIndexedAnnotations( getTargetName() );
		if ( !annotatedMap.containsKey( annName ) ) {
			return;
		}
		List<AnnotationInstance> annotationInstanceList = annotatedMap.get( annName );
		if ( MockHelper.isNotEmpty( annotationInstanceList ) ) {
			for ( AnnotationInstance annotationInstance : annotationInstanceList ) {
				AnnotationTarget annotationTarget = annotationInstance.target();
				if ( MockHelper.targetEquals( target, annotationTarget ) ) {
					if ( operation.process( annotationInstance ) ) {
						return;
					}
				}
			}
		}
	}


	protected AnnotationInstance parserAttributeOverrides(List<JaxbAttributeOverride> attributeOverrides, AnnotationTarget target) {
		if ( target == null ) {
			throw new AssertionFailure( "target can not be null" );
		}
		if ( attributeOverrides == null || attributeOverrides.isEmpty() ) {
			return null;
		}
		Set<String> names = new HashSet<String>();
		for ( JaxbAttributeOverride attributeOverride : attributeOverrides ) {
			names.add( attributeOverride.getName() );
		}
		Operation operation = new AttributeOverrideOperation( names, attributeOverrides );
		getAnnotationInstanceByTarget(
				ATTRIBUTE_OVERRIDES, target, new ContainerOperation( operation )
		);
		getAnnotationInstanceByTarget(
				ATTRIBUTE_OVERRIDE, target, operation
		);
		if ( attributeOverrides.size() == 1 ) {
			return parserAttributeOverride( attributeOverrides.get( 0 ), target );
		}
		else {
			AnnotationValue[] values = new AnnotationValue[attributeOverrides.size()];
			for ( int i = 0; i < values.length; i++ ) {
				values[i] = MockHelper.nestedAnnotationValue(
						"", parserAttributeOverride( attributeOverrides.get( i ), null )
				);
			}
			return create(
					ATTRIBUTE_OVERRIDES,
					target,
					new AnnotationValue[] { AnnotationValue.createArrayValue( "value", values ) }
			);
		}
	}

	protected AnnotationInstance parserAssociationOverrides(List<JaxbAssociationOverride> associationOverrides, AnnotationTarget target) {
		if ( target == null ) {
			throw new AssertionFailure( "target can not be null" );
		}
		if ( associationOverrides == null || associationOverrides.isEmpty() ) {
			return null;
		}

		Set<String> names = new HashSet<String>();
		for ( JaxbAssociationOverride associationOverride : associationOverrides ) {
			names.add( associationOverride.getName() );
		}
		Operation operation = new AssociationOverrideOperation( names, associationOverrides );
		getAnnotationInstanceByTarget(
				ASSOCIATION_OVERRIDES, target, new ContainerOperation( operation )
		);
		getAnnotationInstanceByTarget(
				ASSOCIATION_OVERRIDE, target, operation
		);


		if ( associationOverrides.size() == 1 ) {
			return parserAssociationOverride( associationOverrides.get( 0 ), target );
		}
		else {
			AnnotationValue[] values = new AnnotationValue[associationOverrides.size()];
			for ( int i = 0; i < values.length; i++ ) {
				values[i] = MockHelper.nestedAnnotationValue(
						"", parserAssociationOverride( associationOverrides.get( i ), null )
				);
			}
			return create(
					ASSOCIATION_OVERRIDES,
					target,
					new AnnotationValue[] { AnnotationValue.createArrayValue( "value", values ) }
			);
		}

	}

	protected AnnotationInstance parserCollectionTable(JaxbCollectionTable collectionTable, AnnotationTarget target) {
		if ( collectionTable == null ) {
			return null;
		}
		DefaultConfigurationHelper.INSTANCE.applyDefaults(
				new SchemaAware.CollectionTableSchemaAware( collectionTable ),
				getDefaults()
		);
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", collectionTable.getName(), annotationValueList );
		MockHelper.stringValue( "catalog", collectionTable.getCatalog(), annotationValueList );
		MockHelper.stringValue( "schema", collectionTable.getSchema(), annotationValueList );
		nestedJoinColumnList( "joinColumns", collectionTable.getJoinColumn(), annotationValueList );
		nestedUniqueConstraintList( "uniqueConstraints", collectionTable.getUniqueConstraint(), annotationValueList );
		return create( COLLECTION_TABLE, target, annotationValueList );
	}


	protected AnnotationInstance parserJoinColumnList(List<JaxbJoinColumn> joinColumnList, AnnotationTarget target) {
		if ( MockHelper.isNotEmpty( joinColumnList ) ) {
			if ( joinColumnList.size() == 1 ) {
				return parserJoinColumn( joinColumnList.get( 0 ), target );
			}
			else {
				AnnotationValue[] values = nestedJoinColumnList( "value", joinColumnList, null );
				return create(
						JOIN_COLUMNS,
						target,
						values
				);
			}
		}
		return null;

	}

	protected interface Operation {
		boolean process(AnnotationInstance annotationInstance);
	}

	class ContainerOperation implements Operation {
		private Operation child;

		ContainerOperation(Operation child) {
			this.child = child;
		}

		@Override
		public boolean process(AnnotationInstance annotationInstance) {
			AnnotationValue value = annotationInstance.value();
			AnnotationInstance[] indexedAttributeOverridesValues = value.asNestedArray();
			for ( AnnotationInstance ai : indexedAttributeOverridesValues ) {
				child.process( ai );
			}
			return true;
		}
	}

	class AttributeOverrideOperation implements Operation {
		private Set<String> names;
		private List<JaxbAttributeOverride> attributeOverrides;

		AttributeOverrideOperation(Set<String> names, List<JaxbAttributeOverride> attributeOverrides) {
			this.names = names;
			this.attributeOverrides = attributeOverrides;
		}

		@Override
		public boolean process(AnnotationInstance annotationInstance) {
			String name = annotationInstance.value( "name" ).asString();
			if ( !names.contains( name ) ) {
				JaxbAttributeOverrideProxy attributeOverride = new JaxbAttributeOverrideProxy();
				attributeOverride.setName( name );
				attributeOverride.setColumnAnnotationValue( annotationInstance.value( "column" ) );
				attributeOverrides.add( attributeOverride );
			}
			return false;
		}
	}


	class AssociationOverrideOperation implements Operation {
		private Set<String> names;
		private List<JaxbAssociationOverride> associationOverrides;

		AssociationOverrideOperation(Set<String> names, List<JaxbAssociationOverride> associationOverrides) {
			this.names = names;
			this.associationOverrides = associationOverrides;
		}

		@Override
		public boolean process(AnnotationInstance annotationInstance) {
			String name = annotationInstance.value( "name" ).asString();
			if ( !names.contains( name ) ) {
				JaxbAssociationOverrideProxy associationOverride = new JaxbAssociationOverrideProxy();
				associationOverride.setName( name );
				associationOverride.setJoinColumnsAnnotationValue( annotationInstance.value( "joinColumns" ) );
				associationOverride.setJoinTableAnnotationValue( annotationInstance.value( "joinTable" ) );
				associationOverrides.add( associationOverride );
			}
			return false;
		}

	}

	class JaxbAssociationOverrideProxy extends JaxbAssociationOverride {
		private AnnotationValue joinTableAnnotationValue;
		private AnnotationValue joinColumnsAnnotationValue;

		AnnotationValue getJoinColumnsAnnotationValue() {
			return joinColumnsAnnotationValue;
		}

		void setJoinColumnsAnnotationValue(AnnotationValue joinColumnsAnnotationValue) {
			this.joinColumnsAnnotationValue = joinColumnsAnnotationValue;
		}

		AnnotationValue getJoinTableAnnotationValue() {
			return joinTableAnnotationValue;
		}

		void setJoinTableAnnotationValue(AnnotationValue joinTableAnnotationValue) {
			this.joinTableAnnotationValue = joinTableAnnotationValue;
		}
	}

	class JaxbAttributeOverrideProxy extends JaxbAttributeOverride {
		private AnnotationValue columnAnnotationValue;

		AnnotationValue getColumnAnnotationValue() {
			return columnAnnotationValue;
		}

		void setColumnAnnotationValue(AnnotationValue columnAnnotationValue) {
			this.columnAnnotationValue = columnAnnotationValue;
		}
	}

	/**
	 * Create simple AnnotationInstance with empty annotation value.
	 * AnnotationInstance's target is get from #{getTarget}
	 *
	 * @param name annotation name
	 *
	 * @return annotationInstance which name is , target is from #{getTarget}, and has no annotation values.
	 */
	protected AnnotationInstance create(DotName name) {
		return create( name, MockHelper.EMPTY_ANNOTATION_VALUE_ARRAY );
	}

	protected AnnotationInstance create(DotName name, AnnotationValue[] annotationValues) {
		return create( name, getTarget(), annotationValues );

	}

	protected AnnotationInstance create(DotName name, List<AnnotationValue> annotationValueList) {
		return create( name, getTarget(), annotationValueList );
	}

	/**
	 * @return DotName as the key for the ClassInfo object this mocker created that being push to.
	 */
	abstract protected DotName getTargetName();

	/**
	 * @return Default Annotation Target for #{create} and #{mocker} methods.
	 */
	abstract protected AnnotationTarget getTarget();

	protected AnnotationInstance push(AnnotationInstance annotationInstance) {
		if ( annotationInstance != null && annotationInstance.target() != null ) {
			indexBuilder.addAnnotationInstance( getTargetName(), annotationInstance );
		}
		return annotationInstance;
	}
}
