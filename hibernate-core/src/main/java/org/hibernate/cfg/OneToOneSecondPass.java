/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.cfg;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.cfg.annotations.PropertyBinder;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.type.ForeignKeyDirection;

/**
 * We have to handle OneToOne in a second pass because:
 * -
 */
public class OneToOneSecondPass implements SecondPass {
	private String mappedBy;
	private Mappings mappings;
	private String ownerEntity;
	private String ownerProperty;
	private PropertyHolder propertyHolder;
	private boolean ignoreNotFound;
	private PropertyData inferredData;
	private XClass targetEntity;
	private boolean cascadeOnDelete;
	private boolean optional;
	private String cascadeStrategy;
	private Ejb3JoinColumn[] joinColumns;

	//that suck, we should read that from the property mainly
	public OneToOneSecondPass(
			String mappedBy,
			String ownerEntity,
			String ownerProperty,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			XClass targetEntity,
			boolean ignoreNotFound,
			boolean cascadeOnDelete,
			boolean optional,
			String cascadeStrategy,
			Ejb3JoinColumn[] columns,
			Mappings mappings) {
		this.ownerEntity = ownerEntity;
		this.ownerProperty = ownerProperty;
		this.mappedBy = mappedBy;
		this.propertyHolder = propertyHolder;
		this.mappings = mappings;
		this.ignoreNotFound = ignoreNotFound;
		this.inferredData = inferredData;
		this.targetEntity = targetEntity;
		this.cascadeOnDelete = cascadeOnDelete;
		this.optional = optional;
		this.cascadeStrategy = cascadeStrategy;
		this.joinColumns = columns;
	}

	//TODO refactor this code, there is a lot of duplication in this method
	public void doSecondPass(Map persistentClasses) throws MappingException {
		org.hibernate.mapping.OneToOne value = new org.hibernate.mapping.OneToOne(
				mappings, propertyHolder.getTable(), propertyHolder.getPersistentClass()
		);
		final String propertyName = inferredData.getPropertyName();
		value.setPropertyName( propertyName );
		String referencedEntityName = ToOneBinder.getReferenceEntityName(inferredData, targetEntity, mappings);
		value.setReferencedEntityName( referencedEntityName );  
		AnnotationBinder.defineFetchingStrategy( value, inferredData.getProperty() );
		//value.setFetchMode( fetchMode );
		value.setCascadeDeleteEnabled( cascadeOnDelete );
		//value.setLazy( fetchMode != FetchMode.JOIN );

		if ( !optional ) value.setConstrained( true );
		value.setForeignKeyType(
				value.isConstrained() ?
						ForeignKeyDirection.FOREIGN_KEY_FROM_PARENT :
						ForeignKeyDirection.FOREIGN_KEY_TO_PARENT
		);
		PropertyBinder binder = new PropertyBinder();
		binder.setName( propertyName );
		binder.setValue( value );
		binder.setCascade( cascadeStrategy );
		binder.setAccessType( inferredData.getDefaultAccess() );
		Property prop = binder.makeProperty();
		if ( BinderHelper.isEmptyAnnotationValue( mappedBy ) ) {
			/*
			 * we need to check if the columns are in the right order
			 * if not, then we need to create a many to one and formula
			 * but actually, since entities linked by a one to one need
			 * to share the same composite id class, this cannot happen in hibernate
			 */
			boolean rightOrder = true;

			if ( rightOrder ) {
				String path = StringHelper.qualify( propertyHolder.getPath(), propertyName );
				( new ToOneFkSecondPass(
						value, joinColumns,
						!optional, //cannot have nullabe and unique on certain DBs
						propertyHolder.getEntityOwnerClassName(),
						path, mappings
				) ).doSecondPass( persistentClasses );
				//no column associated since its a one to one
				propertyHolder.addProperty( prop, inferredData.getDeclaringClass() );
			}
			else {
				//this is a many to one with Formula

			}
		}
		else {
			PersistentClass otherSide = (PersistentClass) persistentClasses.get( value.getReferencedEntityName() );
			Property otherSideProperty;
			try {
				if ( otherSide == null ) {
					throw new MappingException( "Unable to find entity: " + value.getReferencedEntityName() );
				}
				otherSideProperty = BinderHelper.findPropertyByName( otherSide, mappedBy );
			}
			catch (MappingException e) {
				throw new AnnotationException(
						"Unknown mappedBy in: " + StringHelper.qualify( ownerEntity, ownerProperty )
								+ ", referenced property unknown: "
								+ StringHelper.qualify( value.getReferencedEntityName(), mappedBy )
				);
			}
			if ( otherSideProperty == null ) {
				throw new AnnotationException(
						"Unknown mappedBy in: " + StringHelper.qualify( ownerEntity, ownerProperty )
								+ ", referenced property unknown: "
								+ StringHelper.qualify( value.getReferencedEntityName(), mappedBy )
				);
			}
			if ( otherSideProperty.getValue() instanceof OneToOne ) {
				propertyHolder.addProperty( prop, inferredData.getDeclaringClass() );
			}
			else if ( otherSideProperty.getValue() instanceof ManyToOne ) {
				Iterator it = otherSide.getJoinIterator();
				Join otherSideJoin = null;
				while ( it.hasNext() ) {
					Join otherSideJoinValue = (Join) it.next();
					if ( otherSideJoinValue.containsProperty( otherSideProperty ) ) {
						otherSideJoin = otherSideJoinValue;
						break;
					}
				}
				if ( otherSideJoin != null ) {
					//@OneToOne @JoinTable
					Join mappedByJoin = buildJoinFromMappedBySide(
							(PersistentClass) persistentClasses.get( ownerEntity ), otherSideProperty, otherSideJoin
					);
					ManyToOne manyToOne = new ManyToOne( mappings, mappedByJoin.getTable() );
					//FIXME use ignore not found here
					manyToOne.setIgnoreNotFound( ignoreNotFound );
					manyToOne.setCascadeDeleteEnabled( value.isCascadeDeleteEnabled() );
					manyToOne.setEmbedded( value.isEmbedded() );
					manyToOne.setFetchMode( value.getFetchMode() );
					manyToOne.setLazy( value.isLazy() );
					manyToOne.setReferencedEntityName( value.getReferencedEntityName() );
					manyToOne.setUnwrapProxy( value.isUnwrapProxy() );
					prop.setValue( manyToOne );
					Iterator otherSideJoinKeyColumns = otherSideJoin.getKey().getColumnIterator();
					while ( otherSideJoinKeyColumns.hasNext() ) {
						Column column = (Column) otherSideJoinKeyColumns.next();
						Column copy = new Column();
						copy.setLength( column.getLength() );
						copy.setScale( column.getScale() );
						copy.setValue( manyToOne );
						copy.setName( column.getQuotedName() );
						copy.setNullable( column.isNullable() );
						copy.setPrecision( column.getPrecision() );
						copy.setUnique( column.isUnique() );
						copy.setSqlType( column.getSqlType() );
						copy.setCheckConstraint( column.getCheckConstraint() );
						copy.setComment( column.getComment() );
						copy.setDefaultValue( column.getDefaultValue() );
						manyToOne.addColumn( copy );
					}
					mappedByJoin.addProperty( prop );
				}
				else {
					propertyHolder.addProperty( prop, inferredData.getDeclaringClass() );
				}

				value.setReferencedPropertyName( mappedBy );

				String propertyRef = value.getReferencedPropertyName();
				if ( propertyRef != null ) {
					mappings.addUniquePropertyReference(
							value.getReferencedEntityName(),
							propertyRef
					);
				}
			}
			else {
				throw new AnnotationException(
						"Referenced property not a (One|Many)ToOne: "
								+ StringHelper.qualify(
								otherSide.getEntityName(), mappedBy
						)
								+ " in mappedBy of "
								+ StringHelper.qualify( ownerEntity, ownerProperty )
				);
			}
		}
		ForeignKey fk = inferredData.getProperty().getAnnotation( ForeignKey.class );
		String fkName = fk != null ? fk.name() : "";
		if ( !BinderHelper.isEmptyAnnotationValue( fkName ) ) value.setForeignKeyName( fkName );
	}

	/**
	 * Builds the <code>Join</code> instance for the mapped by side of a <i>OneToOne</i> association using 
	 * a join tables.
	 * <p>
	 * Note:<br/>
	 * <ul>
	 * <li>From the mappedBy side we should not create the PK nor the FK, this is handled from the other side.</li>
	 * <li>This method is a dirty dupe of EntityBinder.bindSecondaryTable</i>.
	 * </p>
	 */
	private Join buildJoinFromMappedBySide(PersistentClass persistentClass, Property otherSideProperty, Join originalJoin) {
		Join join = new Join();
		join.setPersistentClass( persistentClass );

		//no check constraints available on joins
		join.setTable( originalJoin.getTable() );
		join.setInverse( true );
		SimpleValue key = new DependantValue( mappings, join.getTable(), persistentClass.getIdentifier() );
		//TODO support @ForeignKey
		join.setKey( key );
		join.setSequentialSelect( false );
		//TODO support for inverse and optional
		join.setOptional( true ); //perhaps not quite per-spec, but a Good Thing anyway
		key.setCascadeDeleteEnabled( false );
		Iterator mappedByColumns = otherSideProperty.getValue().getColumnIterator();
		while ( mappedByColumns.hasNext() ) {
			Column column = (Column) mappedByColumns.next();
			Column copy = new Column();
			copy.setLength( column.getLength() );
			copy.setScale( column.getScale() );
			copy.setValue( key );
			copy.setName( column.getQuotedName() );
			copy.setNullable( column.isNullable() );
			copy.setPrecision( column.getPrecision() );
			copy.setUnique( column.isUnique() );
			copy.setSqlType( column.getSqlType() );
			copy.setCheckConstraint( column.getCheckConstraint() );
			copy.setComment( column.getComment() );
			copy.setDefaultValue( column.getDefaultValue() );
			key.addColumn( copy );
		}
		persistentClass.addJoin( join );
		return join;
	}
}

