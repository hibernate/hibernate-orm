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
import java.util.Map;
import javax.persistence.AccessType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.jaxb.mapping.orm.JaxbAccessType;
import org.hibernate.internal.jaxb.mapping.orm.JaxbAttributes;
import org.hibernate.internal.jaxb.mapping.orm.JaxbDiscriminatorColumn;
import org.hibernate.internal.jaxb.mapping.orm.JaxbEntity;
import org.hibernate.internal.jaxb.mapping.orm.JaxbEntityListeners;
import org.hibernate.internal.jaxb.mapping.orm.JaxbIdClass;
import org.hibernate.internal.jaxb.mapping.orm.JaxbInheritance;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPostLoad;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPostPersist;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPostRemove;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPostUpdate;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPrePersist;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPreRemove;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPreUpdate;
import org.hibernate.internal.jaxb.mapping.orm.JaxbSecondaryTable;
import org.hibernate.internal.jaxb.mapping.orm.JaxbTable;
import org.hibernate.internal.util.StringHelper;

/**
 * Mock <entity> to {@link javax.persistence.Entity @Entity}
 *
 * @author Strong Liu
 */
class EntityMocker extends AbstractEntityObjectMocker {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			EntityMocker.class.getName()
	);
	private JaxbEntity entity;

	EntityMocker(IndexBuilder indexBuilder, JaxbEntity entity, EntityMappingsMocker.Default defaults) {
		super( indexBuilder, defaults );
		this.entity = entity;
	}

	@Override
	protected String getClassName() {
		return entity.getClazz();
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
		parserTable( entity.getTable() );
		parserInheritance( entity.getInheritance() );
		parserDiscriminatorColumn( entity.getDiscriminatorColumn() );
		parserAttributeOverrides( entity.getAttributeOverride(), getTarget() );
		parserAssociationOverrides( entity.getAssociationOverride(), getTarget() );
		parserPrimaryKeyJoinColumnList( entity.getPrimaryKeyJoinColumn(), getTarget() );
		parserSecondaryTableList( entity.getSecondaryTable(), getTarget() );

	}

	//@Table  (entity only)
	private AnnotationInstance parserTable(JaxbTable table) {
		if ( table == null ) {
			return null;
		}
		DefaultConfigurationHelper.INSTANCE.applyDefaults(
				new SchemaAware.TableSchemaAware( table ),
				getDefaults()
		);
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", table.getName(), annotationValueList );
		MockHelper.stringValue( "catalog", table.getCatalog(), annotationValueList );
		MockHelper.stringValue( "schema", table.getSchema(), annotationValueList );
		nestedUniqueConstraintList( "uniqueConstraints", table.getUniqueConstraint(), annotationValueList );
		return create( TABLE, annotationValueList );
	}

	protected AccessType getDefaultAccess() {
		if ( entity.getAccess() != null ) {
			return AccessType.valueOf( entity.getAccess().value() );
		}

		return null;
	}

	protected AccessType getAccessFromIndex(DotName className) {
		Map<DotName, List<AnnotationInstance>> indexedAnnotations = indexBuilder.getIndexedAnnotations( className );
		List<AnnotationInstance> accessAnnotationInstances = indexedAnnotations.get( ACCESS );
		if ( MockHelper.isNotEmpty( accessAnnotationInstances ) ) {
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
	protected void applyDefaults() {
		DefaultConfigurationHelper.INSTANCE.applyDefaults( entity, getDefaults() );
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
	protected boolean isMetadataComplete() {
		return entity.isMetadataComplete() != null && entity.isMetadataComplete();
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

	@Override
	protected JaxbAccessType getAccessType() {
		return entity.getAccess();
	}

	//@Inheritance
	protected AnnotationInstance parserInheritance(JaxbInheritance inheritance) {
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
	protected AnnotationInstance parserDiscriminatorColumn(JaxbDiscriminatorColumn discriminatorColumn) {
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
		return
				create(
						DISCRIMINATOR_COLUMN, annotationValueList

				);

	}

	//@SecondaryTable
	protected AnnotationInstance parserSecondaryTable(JaxbSecondaryTable secondaryTable, AnnotationTarget target) {
		if ( secondaryTable == null ) {
			return null;
		}
		DefaultConfigurationHelper.INSTANCE.applyDefaults(
				new SchemaAware.SecondaryTableSchemaAware( secondaryTable ),
				getDefaults()
		);
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", secondaryTable.getName(), annotationValueList );
		MockHelper.stringValue( "catalog", secondaryTable.getCatalog(), annotationValueList );
		MockHelper.stringValue( "schema", secondaryTable.getSchema(), annotationValueList );
		nestedPrimaryKeyJoinColumnList(
				"pkJoinColumns", secondaryTable.getPrimaryKeyJoinColumn(), annotationValueList
		);
		nestedUniqueConstraintList(
				"uniqueConstraints", secondaryTable.getUniqueConstraint(), annotationValueList
		);
		return
				create(
						SECONDARY_TABLE, target, annotationValueList
				);
	}


	protected AnnotationInstance parserSecondaryTableList(List<JaxbSecondaryTable> primaryKeyJoinColumnList, AnnotationTarget target) {
		if ( MockHelper.isNotEmpty( primaryKeyJoinColumnList ) ) {
			if ( primaryKeyJoinColumnList.size() == 1 ) {
				return parserSecondaryTable( primaryKeyJoinColumnList.get( 0 ), target );
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
		if ( MockHelper.isNotEmpty( secondaryTableList ) ) {
			AnnotationValue[] values = new AnnotationValue[secondaryTableList.size()];
			for ( int i = 0; i < secondaryTableList.size(); i++ ) {
				AnnotationInstance annotationInstance = parserSecondaryTable( secondaryTableList.get( i ), null );
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
}
