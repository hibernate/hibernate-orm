/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.EnumSet;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchProfileOverride;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.ToOne;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;

import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.boot.model.internal.BinderHelper.aggregateCascadeTypes;
import static org.hibernate.boot.model.internal.BinderHelper.getFetchMode;
import static org.hibernate.boot.model.internal.BinderHelper.getPath;
import static org.hibernate.boot.model.internal.BinderHelper.isDefault;
import static org.hibernate.boot.model.internal.BinderHelper.handleForeignKeyConstraint;
import static org.hibernate.internal.CoreLogging.messageLogger;
import static org.hibernate.internal.util.StringHelper.isBlank;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;
import static org.hibernate.internal.util.StringHelper.qualify;

/**
 * Responsible for interpreting {@link ManyToOne} and {@link OneToOne} associations
 * and producing mapping model objects of type {@link org.hibernate.mapping.ManyToOne}
 * and {@link org.hibernate.mapping.OneToOne}.
 *
 * @implNote This class is stateless, unlike most of the other "binders".
 *
 * @author Emmanuel Bernard
 */
public class ToOneBinder {
	private static final CoreMessageLogger LOG = messageLogger( ToOneBinder.class );

	static void bindManyToOne(
			PropertyHolder propertyHolder,
			Nullability nullability,
			PropertyData inferredData,
			boolean isIdentifierMapper,
			boolean inSecondPass,
			MetadataBuildingContext context,
			AnnotatedJoinColumns joinColumns,
			PropertyBinder propertyBinder) {
		final var memberDetails = inferredData.getAttributeMember();
		final var manyToOne = memberDetails.getDirectAnnotationUsage( ManyToOne.class );

		//check validity
		if ( memberDetails.hasDirectAnnotationUsage( Column.class )
				|| memberDetails.hasDirectAnnotationUsage( Columns.class ) ) {
			throw new AnnotationException(
					"Property '" + getPath( propertyHolder, inferredData )
							+ "' is a '@ManyToOne' association and may not use '@Column' to specify column mappings (use '@JoinColumn' instead)"
			);
		}

		if ( joinColumns.hasMappedBy()
				&& isIdentifier( propertyHolder, propertyBinder, isIdentifierMapper ) ) {
			throw new AnnotationException(
					"Property '" + getPath( propertyHolder, inferredData )
							+ "' is the inverse side of a '@ManyToOne' association and cannot be used as identifier"
			);
		}

		final var hibernateCascade = memberDetails.getDirectAnnotationUsage( Cascade.class );
		bindManyToOne(
				aggregateCascadeTypes( manyToOne.cascade(), hibernateCascade, false, context ),
				joinColumns,
				propertyHolder,
				nullability,
				inferredData,
				manyToOne.fetch(),
				manyToOne.optional(),
				false,
				isIdentifierMapper,
				inSecondPass,
				propertyBinder,
				context
		);
	}

	private static boolean isIdentifier(
			PropertyHolder propertyHolder,
			PropertyBinder propertyBinder,
			boolean isIdentifierMapper) {
		return propertyBinder.isId()
			|| propertyHolder.isOrWithinEmbeddedId()
			|| propertyHolder.isInIdClass()
			|| isIdentifierMapper;
	}

	private static boolean isMandatory(boolean optional, MemberDetails property, NotFoundAction notFoundAction) {
		// @MapsId means the columns belong to the pk;
		// A @MapsId association (obviously) must be non-null when the entity is first persisted.
		// If a @MapsId association is not mapped with @NotFound(IGNORE), then the association
		// is mandatory (even if the association has optional=true).
		// If a @MapsId association has optional=true and is mapped with @NotFound(IGNORE) then
		// the association is optional.
		// @OneToOne(optional = true) with @PKJC makes the association optional.
		return !optional
			|| property.hasDirectAnnotationUsage( Id.class )
			|| property.hasDirectAnnotationUsage( MapsId.class ) && notFoundAction != NotFoundAction.IGNORE;
	}

	private static void bindManyToOne(
			EnumSet<CascadeType> cascadeStrategy,
			AnnotatedJoinColumns joinColumns,
			PropertyHolder propertyHolder,
			Nullability nullability,
			PropertyData inferredData,
			FetchType fetchType,
			boolean explicitlyOptional,
			boolean unique, // identifies a "logical" @OneToOne
			boolean isIdentifierMapper,
			boolean inSecondPass,
			PropertyBinder propertyBinder,
			MetadataBuildingContext context) {
		final var memberDetails = inferredData.getAttributeMember();

		final var notFoundAction = notFoundAction( propertyHolder, memberDetails, fetchType );
		final var onDeleteAction = onDeleteAction( memberDetails );

		final boolean optional = !isMandatory( explicitlyOptional, memberDetails, notFoundAction );

		final var joinTable = propertyHolder.getJoinTable( memberDetails );
		final var manyToOne =
				joinTable == null
						? new org.hibernate.mapping.ManyToOne( context, joinColumns.getTable() )
						: handleJoinTable( joinColumns, joinTable, notFoundAction, propertyHolder, inferredData, context );

		if ( unique ) {
			// This is a @OneToOne mapped to a physical o.h.mapping.ManyToOne
			manyToOne.markAsLogicalOneToOne();
		}
		final var targetEntityClassDetails = getTargetEntity( inferredData, context );
		manyToOne.setReferencedEntityName( getReferenceEntityName( inferredData, targetEntityClassDetails ) );
		defineFetchingStrategy( manyToOne, memberDetails, inferredData, propertyHolder );
		//value.setFetchMode( fetchMode );
		manyToOne.setNotFoundAction( notFoundAction );
		manyToOne.setOnDeleteAction( onDeleteAction );
		//value.setLazy( fetchMode != FetchMode.JOIN );
		if ( !optional && nullability != Nullability.FORCED_NULL ) {
			for ( AnnotatedJoinColumn column : joinColumns.getJoinColumns() ) {
				column.setNullable( false );
			}
		}

		if ( memberDetails.hasDirectAnnotationUsage( MapsId.class ) ) {
			// read-only
			for ( AnnotatedJoinColumn column : joinColumns.getJoinColumns() ) {
				column.setInsertable( false );
				column.setUpdatable( false );
			}
			joinColumns.setMapsId( memberDetails.getDirectAnnotationUsage( MapsId.class ).value() );
		}

		manyToOne.setTypeName( inferredData.getClassOrElementName() );
		final String propertyName = inferredData.getPropertyName();
		manyToOne.setTypeUsingReflection( propertyHolder.getClassName(), propertyName );

		final String fullPath = qualify( propertyHolder.getPath(), propertyName );

		bindForeignKeyNameAndDefinition( manyToOne, memberDetails, propertyHolder.getOverriddenForeignKey( fullPath ), context );

		final FkSecondPass secondPass = new ToOneFkSecondPass(
				manyToOne,
				joinColumns,
				unique,
				isTargetAnnotatedEntity( targetEntityClassDetails, memberDetails ),
				propertyHolder.getPersistentClass(),
				fullPath,
				context
		);
		if ( inSecondPass ) {
			secondPass.doSecondPass( context.getMetadataCollector().getEntityBindingMap() );
		}
		else {
			context.getMetadataCollector().addSecondPass( secondPass );
		}

		processManyToOneProperty(
				cascadeStrategy,
				joinColumns,
				optional,
				inferredData,
				isIdentifierMapper,
				propertyBinder,
				manyToOne,
				memberDetails,
				propertyName
		);
	}

	private static OnDeleteAction onDeleteAction(MemberDetails property) {
		final var onDelete = property.getDirectAnnotationUsage( OnDelete.class );
		return onDelete == null ? null : onDelete.action();
	}

	private static NotFoundAction notFoundAction(PropertyHolder propertyHolder, MemberDetails property, FetchType fetchType) {
		final var notFound = property.getDirectAnnotationUsage( NotFound.class );
		final NotFoundAction notFoundAction = notFound == null ? null : notFound.action();
		if ( notFoundAction != null && fetchType == LAZY ) {
			LOG.ignoreNotFoundWithFetchTypeLazy( propertyHolder.getEntityName(), property.getName() );
		}
		return notFoundAction;
	}

	private static org.hibernate.mapping.ManyToOne handleJoinTable(
			AnnotatedJoinColumns joinColumns,
			JoinTable joinTable,
			NotFoundAction notFoundAction,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			MetadataBuildingContext context) {
		if ( isBlank( joinTable.name() ) ) {
			// We don't yet know the name of the join table this will map to,
			// so, for now, just treat it as if it maps to the main table
			var value = new org.hibernate.mapping.ManyToOne( context, joinColumns.getTable() );
			context.getMetadataCollector()
					.addSecondPass( new ImplicitToOneJoinTableSecondPass(
							propertyHolder,
							inferredData,
							context,
							joinColumns,
							joinTable,
							notFoundAction,
							value
					) );
			return value;
		}
		else {
			final Join join = propertyHolder.addJoin( joinTable, false );
			for ( var joinColumn : joinColumns.getJoinColumns() ) {
				joinColumn.setExplicitTableName( join.getTable().getName() );
			}
			if ( notFoundAction != null ) {
				join.disableForeignKeyCreation();
			}
			// All FK columns should be in the same table
			return new org.hibernate.mapping.ManyToOne( context, joinColumns.getTable() );
		}
	}

	static boolean isTargetAnnotatedEntity(ClassDetails targetEntity, MemberDetails property) {
		final var target = isDefault( targetEntity ) ? property.getType().determineRawClass() : targetEntity;
		return target.hasDirectAnnotationUsage( Entity.class );
	}

	private static void processManyToOneProperty(
			EnumSet<CascadeType> cascadeStrategy,
			AnnotatedJoinColumns columns,
			boolean optional,
			PropertyData inferredData,
			boolean isIdentifierMapper,
			PropertyBinder propertyBinder,
			org.hibernate.mapping.ManyToOne value,
			MemberDetails property,
			String propertyName) {

		columns.checkPropertyConsistency();

		//PropertyBinder binder = new PropertyBinder();
		propertyBinder.setName( propertyName );
		propertyBinder.setValue( value );
		//binder.setCascade(cascadeStrategy);
		if ( isIdentifierMapper ) {
			propertyBinder.setInsertable( false );
			propertyBinder.setUpdatable( false );
		}
		propertyBinder.setColumns( columns );
		propertyBinder.setAccessType( inferredData.getDefaultAccess() );
		propertyBinder.setCascade( cascadeStrategy );
		propertyBinder.setMemberDetails( property );
		propertyBinder.setToMany( true );

		final var joinColumn = property.getDirectAnnotationUsage( JoinColumn.class );
		final var joinColumns = property.getDirectAnnotationUsage( JoinColumns.class );
		propertyBinder.makePropertyAndBind()
				.setOptional( optional && isNullable( joinColumns, joinColumn ) );
	}

	private static boolean isNullable(JoinColumns joinColumns, JoinColumn joinColumn) {
		if ( joinColumn != null ) {
			return joinColumn.nullable();
		}
		else if ( joinColumns != null ) {
			for ( var column : joinColumns.value() ) {
				if ( column.nullable() ) {
					return true;
				}
			}
			return false;
		}
		else {
			return true;
		}
	}

	static void defineFetchingStrategy(
			ToOne toOne,
			MemberDetails property,
			PropertyData inferredData,
			PropertyHolder propertyHolder) {
		handleLazy( toOne, property );
		handleFetch( toOne, property );
		handleFetchProfileOverrides( toOne, property, propertyHolder, inferredData );
	}

	private static void handleLazy(ToOne toOne, MemberDetails property) {
		if ( property.hasDirectAnnotationUsage( NotFound.class ) ) {
			toOne.setLazy( false );
			toOne.setUnwrapProxy( true );
		}
		else {
			boolean eager = isEager( property );
			toOne.setLazy( !eager );
			toOne.setUnwrapProxy( eager );
			toOne.setUnwrapProxyImplicit( true );
		}
	}

	private static void handleFetchProfileOverrides(
			ToOne toOne,
			MemberDetails property,
			PropertyHolder propertyHolder,
			PropertyData inferredData) {
		final var context = toOne.getBuildingContext();
		final var collector = context.getMetadataCollector();
		final var modelsContext = context.getBootstrapContext().getModelsContext();
		property.forEachAnnotationUsage( FetchProfileOverride.class, modelsContext,
				usage -> collector.addSecondPass( new FetchSecondPass( usage, propertyHolder, inferredData.getPropertyName(), context ) ));
	}

	private static void handleFetch(ToOne toOne, MemberDetails property) {
		final Fetch fetchAnnotationUsage = property.getDirectAnnotationUsage( Fetch.class );
		if ( fetchAnnotationUsage != null ) {
			// Hibernate @Fetch annotation takes precedence
			setHibernateFetchMode( toOne, property, fetchAnnotationUsage.value() );
		}
		else {
			toOne.setFetchMode( getFetchMode( getJpaFetchType( property ) ) );
		}
	}

	private static void setHibernateFetchMode(ToOne toOne, MemberDetails property, org.hibernate.annotations.FetchMode fetchMode) {
		switch ( fetchMode ) {
			case JOIN:
				toOne.setFetchMode( FetchMode.JOIN );
				toOne.setLazy( false );
				toOne.setUnwrapProxy( false );
				break;
			case SELECT:
				toOne.setFetchMode( FetchMode.SELECT );
				break;
			case SUBSELECT:
				throw new AnnotationException( "Association '" + property.getName()
						+ "' is annotated '@Fetch(SUBSELECT)' but is not many-valued");
			default:
				throw new AssertionFailure("unknown fetch type");
		}
	}

	private static boolean isEager(MemberDetails property) {
		return getJpaFetchType( property ) == EAGER;
	}

	private static FetchType getJpaFetchType(MemberDetails property) {
		final var manyToOne = property.getDirectAnnotationUsage( ManyToOne.class );
		final var oneToOne = property.getDirectAnnotationUsage( OneToOne.class );
		if ( manyToOne != null ) {
			return manyToOne.fetch();
		}
		else if ( oneToOne != null ) {
			return oneToOne.fetch();
		}
		else {
			throw new AssertionFailure("Define fetch strategy on a property not annotated with @OneToMany nor @OneToOne");
		}
	}

	static void bindOneToOne(
			PropertyHolder propertyHolder,
			Nullability nullability,
			PropertyData inferredData,
			boolean isIdentifierMapper,
			boolean inSecondPass,
			MetadataBuildingContext context,
			AnnotatedJoinColumns joinColumns,
			PropertyBinder propertyBinder) {
		final var memberDetails = inferredData.getAttributeMember();
		final var oneToOne = memberDetails.getDirectAnnotationUsage( OneToOne.class );

		//check validity
		if ( memberDetails.hasDirectAnnotationUsage( Column.class )
				|| memberDetails.hasDirectAnnotationUsage( Columns.class ) ) {
			throw new AnnotationException(
					"Property '" + getPath( propertyHolder, inferredData )
							+ "' is a '@OneToOne' association and may not use '@Column' to specify column mappings"
							+ " (use '@PrimaryKeyJoinColumn' instead)"
			);
		}

		if ( joinColumns.hasMappedBy() && isIdentifier( propertyHolder, propertyBinder, isIdentifierMapper ) ) {
			throw new AnnotationException(
					"Property '" + getPath( propertyHolder, inferredData )
							+ "' is the inverse side of a '@OneToOne' association and cannot be used as identifier"
			);
		}

		//FIXME support a proper PKJCs
		final boolean trueOneToOne = memberDetails.hasDirectAnnotationUsage( PrimaryKeyJoinColumn.class )
				|| memberDetails.hasDirectAnnotationUsage( PrimaryKeyJoinColumns.class );
		final var hibernateCascade = memberDetails.getDirectAnnotationUsage( Cascade.class );
		bindOneToOne(
				aggregateCascadeTypes( oneToOne.cascade(), hibernateCascade, oneToOne.orphanRemoval(), context ),
				joinColumns,
				oneToOne.optional(),
				oneToOne.fetch(),
				propertyHolder,
				nullability,
				inferredData,
				nullIfEmpty( oneToOne.mappedBy() ),
				trueOneToOne,
				isIdentifierMapper,
				inSecondPass,
				propertyBinder,
				context
		);
	}

	private static void bindOneToOne(
			EnumSet<CascadeType> cascadeStrategy,
			AnnotatedJoinColumns joinColumns,
			boolean explicitlyOptional,
			FetchType fetchMode,
			PropertyHolder propertyHolder,
			Nullability nullability,
			PropertyData inferredData,
			String mappedBy,
			boolean trueOneToOne,
			boolean isIdentifierMapper,
			boolean inSecondPass,
			PropertyBinder propertyBinder,
			MetadataBuildingContext context) {
		//column.getTable() => persistentClass.getTable()
		if ( mappedBy != null || isMappedToPrimaryKey( joinColumns, propertyHolder, trueOneToOne ) ) {
			bindTrueOneToOne(
					cascadeStrategy,
					joinColumns,
					explicitlyOptional,
					fetchMode,
					propertyHolder,
					inferredData,
					mappedBy,
					inSecondPass,
					context
			);
		}
		else {
			//has a FK on the table
			bindManyToOne(
					cascadeStrategy,
					joinColumns,
					propertyHolder,
					nullability,
					inferredData,
					fetchMode,
					explicitlyOptional,
					true,
					isIdentifierMapper,
					inSecondPass,
					propertyBinder,
					context
			);
		}
	}

	private static void bindTrueOneToOne(
			EnumSet<CascadeType> cascadeStrategy,
			AnnotatedJoinColumns joinColumns,
			boolean explicitlyOptional,
			FetchType fetchMode,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			String mappedBy,
			boolean inSecondPass,
			MetadataBuildingContext context) {
		//is a true one-to-one
		//FIXME referencedColumnName ignored => ordering may fail.
		final var memberDetails = inferredData.getAttributeMember();
		final var targetEntityClassDetails = getTargetEntity( inferredData, context );
		final var notFoundAction = notFoundAction( propertyHolder, memberDetails, fetchMode );
		final var secondPass = new OneToOneSecondPass(
				mappedBy,
				propertyHolder.getEntityName(),
				propertyHolder,
				inferredData,
				getReferenceEntityName( inferredData, targetEntityClassDetails ),
				isTargetAnnotatedEntity( targetEntityClassDetails, memberDetails ),
				notFoundAction,
				onDeleteAction( memberDetails ),
				!isMandatory( explicitlyOptional, memberDetails, notFoundAction ),
				cascadeStrategy,
				joinColumns,
				context
		);
		if ( inSecondPass ) {
			secondPass.doSecondPass( context.getMetadataCollector().getEntityBindingMap() );
		}
		else {
			context.getMetadataCollector().addSecondPass( secondPass, mappedBy == null );
		}
	}

	private static boolean isMappedToPrimaryKey(
			AnnotatedJoinColumns joinColumns,
			PropertyHolder propertyHolder,
			boolean trueOneToOne) {
		if ( trueOneToOne ) {
			return true;
		}
		else {
			// Try to find a hidden true one-to-one (FK == PK columns)
			final KeyValue identifier = propertyHolder.getIdentifier();
			if ( identifier == null ) {
				// This is a @OneToOne in an @EmbeddedId
				// (the persistentClass.identifier is not yet set, it's being built)
				// By definition, the PK cannot refer to itself, so it cannot map to itself
				return false;
			}
			else {
				final var columns = joinColumns.getJoinColumns();
				if ( identifier.getColumnSpan() != columns.size() ) {
					return false;
				}
				else {
					final var identifierColumns = identifier.getColumns();
					return columns.stream().noneMatch( column -> {
						final String name = column.getMappingColumn().getName();
						return identifierColumns.stream()
								.noneMatch( idColumn -> idColumn.getName().equals( name ) );
					} );
				}
			}
		}
	}

	public static void bindForeignKeyNameAndDefinition(
			SimpleValue value,
			MemberDetails memberDetails,
			ForeignKey foreignKey,
			MetadataBuildingContext context) {
		if ( memberDetails.hasDirectAnnotationUsage( NotFound.class ) ) {
			// supersedes all others
			value.disableForeignKey();
		}
		else {
			handleForeignKeyConstraint( value, foreignKey, nestedForeignKey( memberDetails ), context );
		}
	}

	private static ForeignKey nestedForeignKey(MemberDetails memberDetails) {
		final var joinColumn = memberDetails.getDirectAnnotationUsage( JoinColumn.class );
		final var joinColumns = memberDetails.getDirectAnnotationUsage( JoinColumns.class );
		if ( joinColumn != null ) {
			return joinColumn.foreignKey();
		}
		else if ( joinColumns != null ) {
			return joinColumns.foreignKey();
		}
		else {
			return null;
		}
	}

	public static String getReferenceEntityName(PropertyData propertyData, ClassDetails targetEntity) {
		return isDefault( targetEntity )
				? propertyData.getClassOrElementName()
				: targetEntity.getName();
	}

	public static String getReferenceEntityName(PropertyData propertyData, MetadataBuildingContext context) {
		return getReferenceEntityName( propertyData, getTargetEntity( propertyData, context ) );
	}

	public static ClassDetails getTargetEntity(PropertyData propertyData, MetadataBuildingContext context) {
		return getTargetEntityClass( propertyData.getAttributeMember(), context );
	}

	private static ClassDetails getTargetEntityClass(MemberDetails property, MetadataBuildingContext context) {
		final var classDetailsRegistry =
				context.getBootstrapContext().getModelsContext().getClassDetailsRegistry();
		final var manyToOne = property.getDirectAnnotationUsage( ManyToOne.class );
		if ( manyToOne != null ) {
			return classDetailsRegistry.resolveClassDetails( manyToOne.targetEntity().getName() );
		}
		final var oneToOne = property.getDirectAnnotationUsage( OneToOne.class );
		if ( oneToOne != null ) {
			return classDetailsRegistry.resolveClassDetails( oneToOne.targetEntity().getName() );
		}
		throw new AssertionFailure( "Unexpected discovery of a targetEntity: " + property.getName() );
	}
}
