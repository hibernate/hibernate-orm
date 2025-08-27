/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.EnumSet;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SortableValue;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.ForeignKeyDirection;

import jakarta.persistence.ForeignKey;

import static org.hibernate.boot.model.internal.BinderHelper.checkMappedByType;
import static org.hibernate.boot.model.internal.BinderHelper.findPropertyByName;
import static org.hibernate.boot.model.internal.BinderHelper.getPath;
import static org.hibernate.boot.model.internal.ToOneBinder.bindForeignKeyNameAndDefinition;
import static org.hibernate.boot.model.internal.ToOneBinder.defineFetchingStrategy;
import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.type.ForeignKeyDirection.FROM_PARENT;
import static org.hibernate.type.ForeignKeyDirection.TO_PARENT;

/**
 * We have to handle {@link jakarta.persistence.OneToOne} associations
 * in a second pass.
 */
public class OneToOneSecondPass implements SecondPass {
	private final PropertyData inferredData;
	private final PropertyHolder propertyHolder;
	private final String mappedBy;
	private final String ownerEntity;
	private final NotFoundAction notFoundAction;
	private final OnDeleteAction onDeleteAction;
	private final boolean optional;
	private final EnumSet<CascadeType> cascadeStrategy;
	private final AnnotatedJoinColumns joinColumns;
	private final MetadataBuildingContext buildingContext;
	private final String referencedEntityName;
	private final boolean annotatedEntity;

	public OneToOneSecondPass(
			String mappedBy,
			String ownerEntity,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			String referencedEntityName,
			boolean annotatedEntity,
			NotFoundAction notFoundAction,
			OnDeleteAction onDeleteAction,
			boolean optional,
			EnumSet<CascadeType> cascadeStrategy,
			AnnotatedJoinColumns columns,
			MetadataBuildingContext buildingContext) {
		this.ownerEntity = ownerEntity;
		this.mappedBy = mappedBy;
		this.propertyHolder = propertyHolder;
		this.referencedEntityName = referencedEntityName;
		this.buildingContext = buildingContext;
		this.notFoundAction = notFoundAction;
		this.inferredData = inferredData;
		this.annotatedEntity = annotatedEntity;
		this.onDeleteAction = onDeleteAction;
		this.optional = optional;
		this.cascadeStrategy = cascadeStrategy;
		this.joinColumns = columns;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		final var oneToOne =
				new OneToOne( buildingContext, propertyHolder.getTable(),
						propertyHolder.getPersistentClass() );
		final String propertyName = inferredData.getPropertyName();
		oneToOne.setPropertyName( propertyName );
		oneToOne.setReferencedEntityName( referencedEntityName );
		MemberDetails property = inferredData.getAttributeMember();
		defineFetchingStrategy( oneToOne, property, inferredData, propertyHolder );
		//value.setFetchMode( fetchMode );
		oneToOne.setOnDeleteAction( onDeleteAction );
		//value.setLazy( fetchMode != FetchMode.JOIN );

		oneToOne.setConstrained( !optional );
		oneToOne.setForeignKeyType( getForeignKeyDirection() );
		bindForeignKeyNameAndDefinition( oneToOne, property,
				property.getDirectAnnotationUsage( ForeignKey.class ),
				buildingContext );

		final var binder = new PropertyBinder();
		binder.setName( propertyName );
		binder.setMemberDetails( property );
		binder.setValue( oneToOne );
		binder.setCascade( cascadeStrategy );
		binder.setAccessType( inferredData.getDefaultAccess() );
		binder.setBuildingContext( buildingContext );
		binder.setHolder( propertyHolder );

		final var lazyGroupAnnotation = property.getDirectAnnotationUsage( LazyGroup.class );
		if ( lazyGroupAnnotation != null ) {
			binder.setLazyGroup( lazyGroupAnnotation.value() );
		}

		final Property result = binder.makeProperty();
		result.setOptional( optional );
		if ( mappedBy == null ) {
			bindOwned( persistentClasses, oneToOne, propertyName, result );
		}
		else {
			bindUnowned( persistentClasses, oneToOne, result );
		}
		binder.callAttributeBindersInSecondPass( result );
		oneToOne.sortProperties();
	}

	private ForeignKeyDirection getForeignKeyDirection() {
		return mappedBy == null ? FROM_PARENT : TO_PARENT;
	}

	private void bindUnowned(Map<String, PersistentClass> persistentClasses, OneToOne oneToOne, Property property) {
		oneToOne.setMappedByProperty( mappedBy );
		final String targetEntityName = oneToOne.getReferencedEntityName();
		final var targetEntity = persistentClasses.get( targetEntityName );
		if ( targetEntity == null ) {
			final String problem = annotatedEntity
					? " which does not belong to the same persistence unit"
					: " which is not an '@Entity' type";
			throw new MappingException( "Association '" + getPath( propertyHolder, inferredData )
					+ "' targets the type '" + targetEntityName + "'" + problem );
		}
		final var targetProperty = targetProperty( oneToOne, targetEntity );
		final var targetPropertyValue = targetProperty.getValue();
		if ( targetPropertyValue instanceof OneToOne ) {
			propertyHolder.addProperty( property, inferredData.getAttributeMember(), inferredData.getDeclaringClass() );
		}
		else if ( targetPropertyValue instanceof ManyToOne ) {
			bindTargetManyToOne( persistentClasses, oneToOne, property, targetEntity, targetProperty );
		}
		else {
			throw new AnnotationException( "Association '" + getPath( propertyHolder, inferredData )
					+ "' is 'mappedBy' a property named '" + mappedBy
					+ "' of the target entity type '" + targetEntityName
					+ "' which is not a '@OneToOne' or '@ManyToOne' association" );
		}
		checkMappedByType(
				mappedBy,
				targetPropertyValue,
				oneToOne.getPropertyName(),
				propertyHolder,
				persistentClasses
		);
	}

	private void bindTargetManyToOne(
			Map<String, PersistentClass> persistentClasses,
			OneToOne oneToOne,
			Property property,
			PersistentClass targetEntity,
			Property targetProperty) {
		Join otherSideJoin = null;
		for ( var otherSideJoinValue : targetEntity.getJoins() ) {
			if ( otherSideJoinValue.containsProperty(targetProperty) ) {
				otherSideJoin = otherSideJoinValue;
				break;
			}
		}
		if ( otherSideJoin != null ) {
			//@OneToOne @JoinTable
			final Join mappedByJoin = buildJoinFromMappedBySide(
					persistentClasses.get( ownerEntity ), targetProperty, otherSideJoin
			);
			final var manyToOne = createManyToOne( oneToOne, mappedByJoin );
			property.setValue( manyToOne );
			for ( var column: otherSideJoin.getKey().getColumns() ) {
				final var copy = column.clone();
				copy.setValue( manyToOne );
				manyToOne.addColumn( copy );
			}
			mappedByJoin.addProperty( property );
		}
		else {
			propertyHolder.addProperty( property, inferredData.getAttributeMember(), inferredData.getDeclaringClass() );
		}

		oneToOne.setReferencedPropertyName( mappedBy );

		// HHH-6813
		// Foo: @Id long id, @OneToOne(mappedBy="foo") Bar bar
		// Bar: @Id @OneToOne Foo foo
		final boolean referenceToPrimaryKey = mappedBy == null
				|| targetEntity.getIdentifier() instanceof Component compositeId
						&& compositeId.matchesAllProperties( mappedBy );
		oneToOne.setReferenceToPrimaryKey( referenceToPrimaryKey );

		final String propertyRef = oneToOne.getReferencedPropertyName();
		if ( propertyRef != null ) {
			buildingContext.getMetadataCollector()
					.addUniquePropertyReference( oneToOne.getReferencedEntityName(), propertyRef );
		}
	}

	private ManyToOne createManyToOne(OneToOne oneToOne, Join mappedByJoin) {
		final ManyToOne manyToOne = new ManyToOne( buildingContext, mappedByJoin.getTable() );
		manyToOne.setNotFoundAction( notFoundAction );
		manyToOne.setOnDeleteAction( oneToOne.getOnDeleteAction() );
		manyToOne.setFetchMode( oneToOne.getFetchMode() );
		manyToOne.setLazy( oneToOne.isLazy() );
		manyToOne.setReferencedEntityName( oneToOne.getReferencedEntityName() );
		manyToOne.setReferencedPropertyName( mappedBy );
		manyToOne.setUnwrapProxy( oneToOne.isUnwrapProxy() );
		manyToOne.markAsLogicalOneToOne();
		return manyToOne;
	}

	private Property targetProperty(OneToOne oneToOne, PersistentClass targetEntity) {
		try {
			final var targetProperty = findPropertyByName( targetEntity, mappedBy );
			if ( targetProperty != null ) {
				return targetProperty;
			}
		}
		catch (MappingException e) {
			// swallow it
		}
		throw new AnnotationException( "Association '" + getPath( propertyHolder, inferredData )
				+ "' is 'mappedBy' a property named '" + mappedBy
				+ "' which does not exist in the target entity type '" + oneToOne.getReferencedEntityName() + "'" );
	}

	private void bindOwned(
			Map<String, PersistentClass> persistentClasses,
			OneToOne oneToOne,
			String propertyName,
			Property property) {
		final ToOneFkSecondPass secondPass = new ToOneFkSecondPass(
				oneToOne,
				joinColumns,
				true,
				annotatedEntity,
				propertyHolder.getPersistentClass(),
				qualify( propertyHolder.getPath(), propertyName ),
				buildingContext
		);
		secondPass.doSecondPass(persistentClasses);
		//no column associated since it's a one to one
		propertyHolder.addProperty( property, inferredData.getAttributeMember(), inferredData.getDeclaringClass() );
	}

	/**
	 * Builds the {@link Join} instance for the unowned side
	 * of a {@code OneToOne} association using a join table.
	 * From the {@code mappedBy} side we should not create
	 * neither the PK, nor the FK, this is all handled from
	 * the owning side.
	 */
	private Join buildJoinFromMappedBySide(PersistentClass persistentClass, Property otherSideProperty, Join originalJoin) {
		final var join = new Join();
		join.setPersistentClass( persistentClass );

		//no check constraints available on joins
		join.setTable( originalJoin.getTable() );
		join.setInverse( true );
		final var key = new DependantValue( buildingContext, join.getTable(), persistentClass.getIdentifier() );

		if ( notFoundAction != null ) {
			join.disableForeignKeyCreation();
		}

		join.setKey( key );
		//perhaps not quite per-spec, but a Good Thing anyway
		join.setOptional( true );
		key.setOnDeleteAction( null );
		for ( var column: otherSideProperty.getValue().getColumns() ) {
			final var copy = column.clone();
			copy.setValue( key );
			key.addColumn( copy );
		}
		if ( otherSideProperty.getValue() instanceof SortableValue value
				&& !value.isSorted() ) {
			key.sortProperties();
		}
		persistentClass.addJoin( join );
		return join;
	}
}
