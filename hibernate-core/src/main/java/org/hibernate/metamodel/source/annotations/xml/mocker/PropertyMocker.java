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

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import org.hibernate.HibernateException;
import org.hibernate.internal.jaxb.mapping.orm.JaxbAccessType;
import org.hibernate.internal.jaxb.mapping.orm.JaxbEnumType;
import org.hibernate.internal.jaxb.mapping.orm.JaxbMapKey;
import org.hibernate.internal.jaxb.mapping.orm.JaxbMapKeyClass;
import org.hibernate.internal.jaxb.mapping.orm.JaxbMapKeyColumn;
import org.hibernate.internal.jaxb.mapping.orm.JaxbMapKeyJoinColumn;
import org.hibernate.internal.jaxb.mapping.orm.JaxbTemporalType;

/**
 * @author Strong Liu
 */
abstract class PropertyMocker extends AnnotationMocker {
	protected ClassInfo classInfo;
	private AnnotationTarget target;

	PropertyMocker(IndexBuilder indexBuilder, ClassInfo classInfo, EntityMappingsMocker.Default defaults) {
		super( indexBuilder, defaults );
		this.classInfo = classInfo;
	}

	protected abstract void processExtra();

	protected abstract String getFieldName();

	protected abstract JaxbAccessType getAccessType();

	protected abstract void setAccessType(JaxbAccessType accessType);

	@Override
	protected DotName getTargetName() {
		return classInfo.name();
	}

	protected void resolveTarget() {
		//attribute in orm.xml has access sub-element
		JaxbAccessType accessType = getAccessType();
		if ( accessType == null ) {
			//attribute in the entity class has @Access
			accessType = AccessHelper.getAccessFromAttributeAnnotation( getTargetName(), getFieldName(), indexBuilder );
			if ( accessType == null ) {
				accessType = AccessHelper.getEntityAccess( getTargetName(), indexBuilder );
			}
			if ( accessType == null ) {
				accessType = AccessHelper.getAccessFromIdPosition( getTargetName(), indexBuilder );
			}
			if (accessType == null ) {
				//this should only for determin @Id position
				accessType = AccessHelper.getAccessFromDefault( indexBuilder );
			}
			if ( accessType == null ) {
				accessType = JaxbAccessType.PROPERTY;

			}
			setAccessType( accessType );
		}

	}

	@Override
	protected AnnotationTarget getTarget() {
		if ( target == null ) {
			target = getTargetFromAttributeAccessType( getAccessType() );
		}
		return target;
	}

	protected AnnotationTarget getTargetFromAttributeAccessType(JaxbAccessType accessType) {
		if ( accessType == null ) {
			throw new IllegalArgumentException( "access type can't be null." );
		}
		switch ( accessType ) {
			case FIELD:
				return MockHelper.getTarget(
						indexBuilder.getServiceRegistry(),
						classInfo,
						getFieldName(),
						MockHelper.TargetType.FIELD
				);
			case PROPERTY:
				return MockHelper.getTarget(
						indexBuilder.getServiceRegistry(),
						classInfo,
						getFieldName(),
						MockHelper.TargetType.PROPERTY
				);
			default:
				throw new HibernateException( "can't determin access type [" + accessType + "]" );
		}
	}


	@Override
	final void process() {
		resolveTarget();
		processExtra();
	}

	protected AnnotationInstance parserMapKeyColumn(JaxbMapKeyColumn mapKeyColumn, AnnotationTarget target) {
		if ( mapKeyColumn == null ) {
			return null;
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", mapKeyColumn.getName(), annotationValueList );
		MockHelper.stringValue( "columnDefinition", mapKeyColumn.getColumnDefinition(), annotationValueList );
		MockHelper.stringValue( "table", mapKeyColumn.getTable(), annotationValueList );
		MockHelper.booleanValue( "nullable", mapKeyColumn.isNullable(), annotationValueList );
		MockHelper.booleanValue( "insertable", mapKeyColumn.isInsertable(), annotationValueList );
		MockHelper.booleanValue( "updatable", mapKeyColumn.isUpdatable(), annotationValueList );
		MockHelper.booleanValue( "unique", mapKeyColumn.isUnique(), annotationValueList );
		MockHelper.integerValue( "length", mapKeyColumn.getLength(), annotationValueList );
		MockHelper.integerValue( "precision", mapKeyColumn.getPrecision(), annotationValueList );
		MockHelper.integerValue( "scale", mapKeyColumn.getScale(), annotationValueList );
		return create( MAP_KEY_COLUMN, target, annotationValueList );
	}

	protected AnnotationInstance parserMapKeyClass(JaxbMapKeyClass mapKeyClass, AnnotationTarget target) {
		if ( mapKeyClass == null ) {
			return null;
		}
		return create(
				MAP_KEY_CLASS, target, MockHelper.classValueArray(
				"value", mapKeyClass.getClazz(), indexBuilder.getServiceRegistry()
		)
		);
	}

	protected AnnotationInstance parserMapKeyTemporal(JaxbTemporalType temporalType, AnnotationTarget target) {
		if ( temporalType == null ) {
			return null;
		}
		return create(
				MAP_KEY_TEMPORAL, target,
				MockHelper.enumValueArray( "value", TEMPORAL_TYPE, temporalType )
		);
	}

	protected AnnotationInstance parserMapKeyEnumerated(JaxbEnumType enumType, AnnotationTarget target) {
		if ( enumType == null ) {
			return null;
		}
		return create(
				MAP_KEY_ENUMERATED, target,
				MockHelper.enumValueArray( "value", ENUM_TYPE, enumType )
		);
	}

	protected AnnotationInstance parserMapKey(JaxbMapKey mapKey, AnnotationTarget target) {
		if ( mapKey == null ) {
			return null;
		}
		return create( MAP_KEY, target, MockHelper.stringValueArray( "name", mapKey.getName() ) );
	}

	private AnnotationValue[] nestedMapKeyJoinColumnList(String name, List<JaxbMapKeyJoinColumn> columns, List<AnnotationValue> annotationValueList) {
		if ( MockHelper.isNotEmpty( columns ) ) {
			AnnotationValue[] values = new AnnotationValue[columns.size()];
			for ( int i = 0; i < columns.size(); i++ ) {
				AnnotationInstance annotationInstance = parserMapKeyJoinColumn( columns.get( i ), null );
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

	protected AnnotationInstance parserMapKeyJoinColumnList(List<JaxbMapKeyJoinColumn> joinColumnList, AnnotationTarget target) {
		if ( MockHelper.isNotEmpty( joinColumnList ) ) {
			if ( joinColumnList.size() == 1 ) {
				return parserMapKeyJoinColumn( joinColumnList.get( 0 ), target );
			}
			else {
				AnnotationValue[] values = nestedMapKeyJoinColumnList( "value", joinColumnList, null );
				return create(
						MAP_KEY_JOIN_COLUMNS,
						target,
						values
				);
			}
		}
		return null;

	}

	//@MapKeyJoinColumn
	private AnnotationInstance parserMapKeyJoinColumn(JaxbMapKeyJoinColumn column, AnnotationTarget target) {
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
		return create( MAP_KEY_JOIN_COLUMN, target, annotationValueList );
	}


}
