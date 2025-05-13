/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

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
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.ToOne;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;

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
import static org.hibernate.boot.model.internal.BinderHelper.noConstraint;
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
			PropertyData inferredData,
			boolean isIdentifierMapper,
			boolean inSecondPass,
			MetadataBuildingContext context,
			MemberDetails property,
			AnnotatedJoinColumns joinColumns,
			PropertyBinder propertyBinder) {
		final ManyToOne manyToOne = property.getDirectAnnotationUsage( ManyToOne.class );

		//check validity
		if ( property.hasDirectAnnotationUsage( Column.class )
				|| property.hasDirectAnnotationUsage( Columns.class ) ) {
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

		final Cascade hibernateCascade = property.getDirectAnnotationUsage( Cascade.class );
		final NotFound notFound = property.getDirectAnnotationUsage( NotFound.class );
		final NotFoundAction notFoundAction = notFound == null ? null : notFound.action();
		matchIgnoreNotFoundWithFetchType( propertyHolder.getEntityName(), property.getName(), notFoundAction, manyToOne.fetch() );
		final OnDelete onDelete = property.getDirectAnnotationUsage( OnDelete.class );
		final JoinTable joinTable = propertyHolder.getJoinTable( property );
		bindManyToOne(
				aggregateCascadeTypes( manyToOne.cascade(), hibernateCascade, false, context ),
				joinColumns,
				joinTable,
				!isMandatory( manyToOne.optional(), property, notFoundAction ),
				notFoundAction,
				onDelete == null ? null : onDelete.action(),
				getTargetEntity( inferredData, context ),
				propertyHolder,
				inferredData,
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
			JoinTable joinTable,
			boolean optional,
			NotFoundAction notFoundAction,
			OnDeleteAction onDeleteAction,
			ClassDetails targetEntity,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			boolean unique, // identifies a "logical" @OneToOne
			boolean isIdentifierMapper,
			boolean inSecondPass,
			PropertyBinder propertyBinder,
			MetadataBuildingContext context) {
		if ( joinTable != null && !isBlank( joinTable.name() ) ) {
			final Join join = propertyHolder.addJoin( joinTable, false );
			// TODO: if notFoundAction!=null should we call join.disableForeignKeyCreation() ?
			for ( AnnotatedJoinColumn joinColumn : joinColumns.getJoinColumns() ) {
				joinColumn.setExplicitTableName( join.getTable().getName() );
			}
			if ( notFoundAction != null ) {
				join.disableForeignKeyCreation();
			}
		}

		// All FK columns should be in the same table
		final org.hibernate.mapping.ManyToOne value =
				new org.hibernate.mapping.ManyToOne( context, joinColumns.getTable() );

		if ( joinTable != null && isBlank( joinTable.name() ) ) {
			context.getMetadataCollector().addSecondPass( new ImplicitToOneJoinTableSecondPass(
					propertyHolder,
					inferredData,
					context,
					joinColumns,
					joinTable,
					notFoundAction,
					value
			) );
		}

		if ( unique ) {
			// This is a @OneToOne mapped to a physical o.h.mapping.ManyToOne
			value.markAsLogicalOneToOne();
		}
		value.setReferencedEntityName( getReferenceEntityName( inferredData, targetEntity ) );
		final MemberDetails property = inferredData.getAttributeMember();
		defineFetchingStrategy( value, property, inferredData, propertyHolder );
		//value.setFetchMode( fetchMode );
		value.setNotFoundAction( notFoundAction );
		value.setOnDeleteAction( onDeleteAction );
		//value.setLazy( fetchMode != FetchMode.JOIN );
		if ( !optional ) {
			for ( AnnotatedJoinColumn column : joinColumns.getJoinColumns() ) {
				column.setNullable( false );
			}
		}

		if ( property.hasDirectAnnotationUsage( MapsId.class ) ) {
			final MapsId mapsId = property.getDirectAnnotationUsage( MapsId.class );
			final List<AnnotatedJoinColumn> joinColumnList = joinColumns.getJoinColumns();
			//read only
			for ( AnnotatedJoinColumn column : joinColumnList ) {
				column.setInsertable( false );
				column.setUpdatable( false );
			}
			joinColumns.setMapsId( mapsId.value() );
		}

		value.setTypeName( inferredData.getClassOrElementName() );
		final String propertyName = inferredData.getPropertyName();
		value.setTypeUsingReflection( propertyHolder.getClassName(), propertyName );

		final String fullPath = qualify( propertyHolder.getPath(), propertyName );

		bindForeignKeyNameAndDefinition( value, property, propertyHolder.getOverriddenForeignKey( fullPath ), context );

		final FkSecondPass secondPass = new ToOneFkSecondPass(
				value,
				joinColumns,
				unique,
				isTargetAnnotatedEntity( targetEntity, property ),
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
				value,
				property,
				propertyName
		);
	}

	static boolean isTargetAnnotatedEntity(ClassDetails targetEntity, MemberDetails property) {
		final ClassDetails target = isDefault( targetEntity ) ? property.getType().determineRawClass() : targetEntity;
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

		final JoinColumn joinColumn = property.getDirectAnnotationUsage( JoinColumn.class );
		final JoinColumns joinColumns = property.getDirectAnnotationUsage( JoinColumns.class );
		propertyBinder.makePropertyAndBind()
				.setOptional( optional && isNullable( joinColumns, joinColumn ) );
	}

	private static boolean isNullable(JoinColumns joinColumns, JoinColumn joinColumn) {
		if ( joinColumn != null ) {
			return joinColumn.nullable();
		}
		else if ( joinColumns != null ) {
			for ( JoinColumn column : joinColumns.value() ) {
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
		final MetadataBuildingContext context = toOne.getBuildingContext();
		final InFlightMetadataCollector collector = context.getMetadataCollector();
		final ModelsContext modelsContext = context.getBootstrapContext().getModelsContext();
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
		final ManyToOne manyToOne = property.getDirectAnnotationUsage( ManyToOne.class );
		final OneToOne oneToOne = property.getDirectAnnotationUsage( OneToOne.class );
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
			PropertyData inferredData,
			boolean isIdentifierMapper,
			boolean inSecondPass,
			MetadataBuildingContext context,
			MemberDetails property,
			AnnotatedJoinColumns joinColumns,
			PropertyBinder propertyBinder) {
		final OneToOne oneToOne = property.getDirectAnnotationUsage( OneToOne.class );

		//check validity
		if ( property.hasDirectAnnotationUsage( Column.class )
				|| property.hasDirectAnnotationUsage( Columns.class ) ) {
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
		final boolean trueOneToOne = property.hasDirectAnnotationUsage( PrimaryKeyJoinColumn.class )
				|| property.hasDirectAnnotationUsage( PrimaryKeyJoinColumns.class );
		final Cascade hibernateCascade = property.getDirectAnnotationUsage( Cascade.class );
		final NotFound notFound = property.getDirectAnnotationUsage( NotFound.class );
		final NotFoundAction notFoundAction = notFound == null ? null : notFound.action();

		matchIgnoreNotFoundWithFetchType( propertyHolder.getEntityName(), property.getName(), notFoundAction, oneToOne.fetch() );
		final OnDelete onDelete = property.getDirectAnnotationUsage( OnDelete.class );
		final JoinTable joinTable = propertyHolder.getJoinTable( property );
		bindOneToOne(
				aggregateCascadeTypes( oneToOne.cascade(), hibernateCascade, oneToOne.orphanRemoval(), context ),
				joinColumns,
				joinTable,
				!isMandatory( oneToOne.optional(), property, notFoundAction ),
				getFetchMode( oneToOne.fetch() ),
				notFoundAction,
				onDelete == null ? null : onDelete.action(),
				getTargetEntity( inferredData, context ),
				property,
				propertyHolder,
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
			JoinTable joinTable,
			boolean optional,
			FetchMode fetchMode,
			NotFoundAction notFoundAction,
			OnDeleteAction cascadeOnDelete,
			ClassDetails targetEntity,
			MemberDetails annotatedProperty,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			String mappedBy,
			boolean trueOneToOne,
			boolean isIdentifierMapper,
			boolean inSecondPass,
			PropertyBinder propertyBinder,
			MetadataBuildingContext context) {
		//column.getTable() => persistentClass.getTable()
		final String propertyName = inferredData.getPropertyName();
		LOG.tracev( "Fetching {0} with {1}", propertyName, fetchMode );
		if ( isMapToPK( joinColumns, propertyHolder, trueOneToOne )
				|| mappedBy != null ) {
			//is a true one-to-one
			//FIXME referencedColumnName ignored => ordering may fail.
			final OneToOneSecondPass secondPass = new OneToOneSecondPass(
					mappedBy,
					propertyHolder.getEntityName(),
					propertyHolder,
					inferredData,
					getReferenceEntityName( inferredData, targetEntity ),
					isTargetAnnotatedEntity( targetEntity, annotatedProperty ),
					notFoundAction,
					cascadeOnDelete,
					optional,
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
		else {
			//has a FK on the table
			bindManyToOne(
					cascadeStrategy,
					joinColumns,
					joinTable, optional,
					notFoundAction,
					cascadeOnDelete,
					targetEntity,
					propertyHolder,
					inferredData,
					true,
					isIdentifierMapper,
					inSecondPass,
					propertyBinder,
					context
			);
		}
	}

	private static boolean isMapToPK(AnnotatedJoinColumns joinColumns, PropertyHolder propertyHolder, boolean trueOneToOne) {
		if ( trueOneToOne ) {
			return true;
		}
		else {
			//try to find a hidden true one to one (FK == PK columns)
			final KeyValue identifier = propertyHolder.getIdentifier();
			if ( identifier == null ) {
				//this is a @OneToOne in an @EmbeddedId (the persistentClass.identifier is not set yet, it's being built)
				//by definition the PK cannot refer to itself so it cannot map to itself
				return false;
			}
			else {
				final List<String> idColumnNames = new ArrayList<>();
				final List<AnnotatedJoinColumn> columns = joinColumns.getJoinColumns();
				if ( identifier.getColumnSpan() != columns.size() ) {
					return false;
				}
				else {
					for ( org.hibernate.mapping.Column currentColumn: identifier.getColumns() ) {
						idColumnNames.add( currentColumn.getName() );
					}
					for ( AnnotatedJoinColumn column: columns ) {
						if ( !idColumnNames.contains( column.getMappingColumn().getName() ) ) {
							return false;
						}
					}
					return true;
				}
			}
		}
	}

	public static void bindForeignKeyNameAndDefinition(
			SimpleValue value,
			MemberDetails property,
			ForeignKey foreignKey,
			MetadataBuildingContext context) {
		if ( property.hasDirectAnnotationUsage( NotFound.class ) ) {
			// supersedes all others
			value.disableForeignKey();
		}
		else {
			final JoinColumn joinColumn = property.getDirectAnnotationUsage( JoinColumn.class );
			final JoinColumns joinColumns = property.getDirectAnnotationUsage( JoinColumns.class );
			final boolean noConstraintByDefault = context.getBuildingOptions().isNoConstraintByDefault();
			if ( joinColumn != null && noConstraint( joinColumn.foreignKey(), noConstraintByDefault )
					|| joinColumns != null && noConstraint( joinColumns.foreignKey(), noConstraintByDefault ) ) {
				value.disableForeignKey();
			}
			else {
				if ( noConstraint( foreignKey, noConstraintByDefault ) ) {
					value.disableForeignKey();
				}
				else if ( foreignKey != null ) {
					value.setForeignKeyName( nullIfEmpty( foreignKey.name() ) );
					value.setForeignKeyDefinition( nullIfEmpty( foreignKey.foreignKeyDefinition() ) );
					value.setForeignKeyOptions( foreignKey.options() );
				}
				else if ( noConstraintByDefault ) {
					value.disableForeignKey();
				}
				else if ( joinColumns != null ) {
					final ForeignKey joinColumnsForeignKey = joinColumns.foreignKey();
					value.setForeignKeyName( nullIfEmpty( joinColumnsForeignKey.name() ) );
					value.setForeignKeyDefinition( nullIfEmpty( joinColumnsForeignKey.foreignKeyDefinition() ) );
					value.setForeignKeyOptions( joinColumnsForeignKey.options() );
				}
				else if ( joinColumn != null ) {
					final ForeignKey joinColumnForeignKey = joinColumn.foreignKey();
					value.setForeignKeyName( nullIfEmpty( joinColumnForeignKey.name() ) );
					value.setForeignKeyDefinition( nullIfEmpty( joinColumnForeignKey.foreignKeyDefinition() ) );
					value.setForeignKeyOptions( joinColumnForeignKey.options() );
				}
			}
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
		final ModelsContext modelsContext = context.getBootstrapContext().getModelsContext();
		final ManyToOne manyToOne = property.getDirectAnnotationUsage( ManyToOne.class );
		if ( manyToOne != null ) {
			return modelsContext.getClassDetailsRegistry().resolveClassDetails( manyToOne.targetEntity().getName() );
		}
		final OneToOne oneToOne = property.getDirectAnnotationUsage( OneToOne.class );
		if ( oneToOne != null ) {
			return modelsContext.getClassDetailsRegistry().resolveClassDetails( oneToOne.targetEntity().getName() );
		}
		throw new AssertionFailure( "Unexpected discovery of a targetEntity: " + property.getName() );
	}

	private static void matchIgnoreNotFoundWithFetchType(
			String entity,
			String association,
			NotFoundAction notFoundAction,
			FetchType fetchType) {
		if ( notFoundAction != null && fetchType == LAZY ) {
			LOG.ignoreNotFoundWithFetchTypeLazy( entity, association );
		}
	}
}
