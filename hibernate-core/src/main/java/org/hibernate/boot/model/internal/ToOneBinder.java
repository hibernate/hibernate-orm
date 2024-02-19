/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchProfileOverride;
import org.hibernate.annotations.FetchProfileOverrides;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.ToOne;

import jakarta.persistence.Column;
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
import static org.hibernate.boot.model.internal.BinderHelper.getCascadeStrategy;
import static org.hibernate.boot.model.internal.BinderHelper.getFetchMode;
import static org.hibernate.boot.model.internal.BinderHelper.getPath;
import static org.hibernate.boot.model.internal.BinderHelper.isDefault;
import static org.hibernate.boot.model.internal.BinderHelper.noConstraint;
import static org.hibernate.internal.CoreLogging.messageLogger;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
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
			XProperty property,
			AnnotatedJoinColumns joinColumns,
			PropertyBinder propertyBinder,
			boolean forcePersist) {
		final ManyToOne manyToOne = property.getAnnotation( ManyToOne.class );

		//check validity
		if ( property.isAnnotationPresent( Column.class )
				|| property.isAnnotationPresent( Columns.class ) ) {
			throw new AnnotationException(
					"Property '" + getPath( propertyHolder, inferredData )
							+ "' is a '@ManyToOne' association and may not use '@Column' to specify column mappings (use '@JoinColumn' instead)"
			);
		}

		if ( joinColumns.hasMappedBy() && isIdentifier( propertyHolder, propertyBinder, isIdentifierMapper ) ) {
			throw new AnnotationException(
					"Property '" + getPath( propertyHolder, inferredData )
							+ "' is the inverse side of a '@ManyToOne' association and cannot be used as identifier"
			);
		}

		final Cascade hibernateCascade = property.getAnnotation( Cascade.class );
		final NotFound notFound = property.getAnnotation( NotFound.class );
		final NotFoundAction notFoundAction = notFound == null ? null : notFound.action();
		matchIgnoreNotFoundWithFetchType( propertyHolder.getEntityName(), property.getName(), notFoundAction, manyToOne.fetch() );
		final OnDelete onDelete = property.getAnnotation( OnDelete.class );
		final JoinTable joinTable = propertyHolder.getJoinTable( property );
		bindManyToOne(
				getCascadeStrategy( manyToOne.cascade(), hibernateCascade, false, forcePersist ),
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

	private static boolean isMandatory(boolean optional, XProperty property, NotFoundAction notFoundAction) {
		// @MapsId means the columns belong to the pk;
		// A @MapsId association (obviously) must be non-null when the entity is first persisted.
		// If a @MapsId association is not mapped with @NotFound(IGNORE), then the association
		// is mandatory (even if the association has optional=true).
		// If a @MapsId association has optional=true and is mapped with @NotFound(IGNORE) then
		// the association is optional.
		// @OneToOne(optional = true) with @PKJC makes the association optional.
		return !optional
			|| property.isAnnotationPresent( Id.class )
			|| property.isAnnotationPresent( MapsId.class ) && notFoundAction != NotFoundAction.IGNORE;
	}

	private static void bindManyToOne(
			String cascadeStrategy,
			AnnotatedJoinColumns joinColumns,
			JoinTable joinTable,
			boolean optional,
			NotFoundAction notFoundAction,
			OnDeleteAction onDeleteAction,
			XClass targetEntity,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			boolean unique, // identifies a "logical" @OneToOne
			boolean isIdentifierMapper,
			boolean inSecondPass,
			PropertyBinder propertyBinder,
			MetadataBuildingContext context) {

		if ( joinTable != null && !isEmpty( joinTable.name() ) ) {
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

		if ( joinTable != null && isEmpty( joinTable.name() ) ) {
			context.getMetadataCollector()
					.addSecondPass( new ImplicitToOneJoinTableSecondPass( propertyHolder, inferredData, context,
							joinColumns, joinTable, notFoundAction, value ) );
		}

		if ( unique ) {
			// This is a @OneToOne mapped to a physical o.h.mapping.ManyToOne
			value.markAsLogicalOneToOne();
		}
		value.setReferencedEntityName( getReferenceEntityName( inferredData, targetEntity, context ) );
		final XProperty property = inferredData.getProperty();
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

		if ( property.isAnnotationPresent( MapsId.class ) ) {
			final MapsId mapsId = property.getAnnotation(MapsId.class);
			final List<AnnotatedJoinColumn> joinColumnList = joinColumns.getJoinColumns();
			//read only
			for ( AnnotatedJoinColumn column : joinColumnList ) {
				column.setInsertable( false );
				column.setUpdatable( false );
			}
			joinColumns.setMapsId( mapsId.value() );
		}

		boolean hasSpecjManyToOne = handleSpecjSyntax( joinColumns, inferredData, context, property );
		value.setTypeName( inferredData.getClassOrElementName() );
		final String propertyName = inferredData.getPropertyName();
		value.setTypeUsingReflection( propertyHolder.getClassName(), propertyName );

		final String fullPath = qualify( propertyHolder.getPath(), propertyName );

		bindForeignKeyNameAndDefinition( value, property, propertyHolder.getOverriddenForeignKey( fullPath ), context );

		final FkSecondPass secondPass = new ToOneFkSecondPass(
				value,
				joinColumns,
				unique,
				isTargetAnnotatedEntity( targetEntity, property, context ),
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
				hasSpecjManyToOne,
				propertyName
		);
	}

	static boolean isTargetAnnotatedEntity(XClass targetEntity, XProperty property, MetadataBuildingContext context) {
		final XClass target = isDefault( targetEntity, context ) ? property.getType() : targetEntity;
		return target.isAnnotationPresent( Entity.class );
	}

	private static boolean handleSpecjSyntax(
			AnnotatedJoinColumns columns,
			PropertyData inferredData,
			MetadataBuildingContext context,
			XProperty property) {
		//Make sure that JPA1 key-many-to-one columns are read only too
		boolean hasSpecjManyToOne = false;
		if ( context.getBuildingOptions().isSpecjProprietarySyntaxEnabled() ) {
			final JoinColumn joinColumn = property.getAnnotation( JoinColumn.class );
			String columnName = "";
			for ( XProperty prop : inferredData.getDeclaringClass()
					.getDeclaredProperties( AccessType.FIELD.getType() ) ) {
				if ( prop.isAnnotationPresent( Id.class ) && prop.isAnnotationPresent( Column.class ) ) {
					columnName = prop.getAnnotation( Column.class ).name();
				}

				if ( property.isAnnotationPresent( ManyToOne.class ) && joinColumn != null ) {
					if ( !joinColumn.name().isEmpty()
							&& joinColumn.name().equals( columnName )
							&& !property.isAnnotationPresent( MapsId.class ) ) {
						hasSpecjManyToOne = true;
						for ( AnnotatedJoinColumn column : columns.getJoinColumns() ) {
							column.setInsertable( false );
							column.setUpdatable( false );
						}
					}
				}
			}
		}
		return hasSpecjManyToOne;
	}

	private static void processManyToOneProperty(
			String cascadeStrategy,
			AnnotatedJoinColumns columns,
			boolean optional,
			PropertyData inferredData,
			boolean isIdentifierMapper,
			PropertyBinder propertyBinder,
			org.hibernate.mapping.ManyToOne value,
			XProperty property,
			boolean hasSpecjManyToOne,
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
		else if ( hasSpecjManyToOne ) {
			propertyBinder.setInsertable( false );
			propertyBinder.setUpdatable( false );
		}
		propertyBinder.setColumns( columns );
		propertyBinder.setAccessType( inferredData.getDefaultAccess() );
		propertyBinder.setCascade( cascadeStrategy );
		propertyBinder.setProperty( property );
		propertyBinder.setToMany( true );

		final JoinColumn joinColumn = property.getAnnotation( JoinColumn.class );
		final JoinColumns joinColumns = property.getAnnotation( JoinColumns.class );
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
			XProperty property,
			PropertyData inferredData,
			PropertyHolder propertyHolder) {
		handleLazy( toOne, property, inferredData, propertyHolder );
		handleFetch( toOne, property );
		handleFetchProfileOverrides( toOne, property, propertyHolder, inferredData );
	}

	private static void handleLazy(ToOne toOne, XProperty property, PropertyData inferredData, PropertyHolder propertyHolder) {
		if ( property.isAnnotationPresent( NotFound.class ) ) {
			toOne.setLazy( false );
			toOne.setUnwrapProxy( true );
		}
		else {
			boolean eager = isEager( property, inferredData, propertyHolder );
			toOne.setLazy( !eager );
			toOne.setUnwrapProxy( eager );
			toOne.setUnwrapProxyImplicit( true );
		}
	}

	private static void handleFetchProfileOverrides(
			ToOne toOne,
			XProperty property,
			PropertyHolder propertyHolder,
			PropertyData inferredData) {
		final MetadataBuildingContext context = toOne.getBuildingContext();
		final InFlightMetadataCollector collector = context.getMetadataCollector();
		if ( property.isAnnotationPresent( FetchProfileOverride.class ) ) {
			final FetchProfileOverride fetch = property.getAnnotation( FetchProfileOverride.class );
			collector.addSecondPass( new FetchSecondPass( fetch, propertyHolder, inferredData.getPropertyName(), context ) );
		}
		else if ( property.isAnnotationPresent( FetchProfileOverrides.class ) ) {
			for ( FetchProfileOverride fetch: property.getAnnotation( FetchProfileOverrides.class ).value() ) {
				collector.addSecondPass( new FetchSecondPass( fetch, propertyHolder, inferredData.getPropertyName(), context ) );
			}
		}
	}

	private static void handleFetch(ToOne toOne, XProperty property) {
		if ( property.isAnnotationPresent( Fetch.class ) ) {
			// Hibernate @Fetch annotation takes precedence
			setHibernateFetchMode( toOne, property, property.getAnnotation( Fetch.class ).value() );
		}
		else {
			toOne.setFetchMode( getFetchMode( getJpaFetchType( property ) ) );
		}
	}

	private static void setHibernateFetchMode(ToOne toOne, XProperty property, org.hibernate.annotations.FetchMode fetchMode) {
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

	private static boolean isEager(XProperty property, PropertyData inferredData, PropertyHolder propertyHolder) {
		final FetchType fetchType = getJpaFetchType( property );
		if ( property.isAnnotationPresent( LazyToOne.class ) ) {
			// LazyToOne takes precedent
			final LazyToOne lazy = property.getAnnotation( LazyToOne.class );
			boolean eager = lazy.value() == LazyToOneOption.FALSE;
			if ( eager && fetchType == LAZY ) {
				// conflicts with non-default setting
				throw new AnnotationException("Association '" + getPath(propertyHolder, inferredData)
						+ "' is marked 'fetch=LAZY' and '@LazyToOne(FALSE)'");
			}
			return eager;
		}
		else {
			return fetchType == EAGER;
		}
	}

	private static FetchType getJpaFetchType(XProperty property) {
		final ManyToOne manyToOne = property.getAnnotation( ManyToOne.class );
		final OneToOne oneToOne = property.getAnnotation( OneToOne.class );
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
			XProperty property,
			AnnotatedJoinColumns joinColumns,
			PropertyBinder propertyBinder,
			boolean forcePersist) {
		final OneToOne oneToOne = property.getAnnotation( OneToOne.class );

		//check validity
		if ( property.isAnnotationPresent( Column.class )
				|| property.isAnnotationPresent( Columns.class ) ) {
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
		final boolean trueOneToOne = property.isAnnotationPresent( PrimaryKeyJoinColumn.class )
				|| property.isAnnotationPresent( PrimaryKeyJoinColumns.class );
		final Cascade hibernateCascade = property.getAnnotation( Cascade.class );
		final NotFound notFound = property.getAnnotation( NotFound.class );
		final NotFoundAction notFoundAction = notFound == null ? null : notFound.action();

		matchIgnoreNotFoundWithFetchType( propertyHolder.getEntityName(), property.getName(), notFoundAction, oneToOne.fetch() );
		final OnDelete onDelete = property.getAnnotation( OnDelete.class );
		final JoinTable joinTable = propertyHolder.getJoinTable(property);
		bindOneToOne(
				getCascadeStrategy( oneToOne.cascade(), hibernateCascade, oneToOne.orphanRemoval(), forcePersist ),
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
			String cascadeStrategy,
			AnnotatedJoinColumns joinColumns,
			JoinTable joinTable,
			boolean optional,
			FetchMode fetchMode,
			NotFoundAction notFoundAction,
			OnDeleteAction cascadeOnDelete,
			XClass targetEntity,
			XProperty annotatedProperty,
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
					getReferenceEntityName( inferredData, targetEntity, context ),
					isTargetAnnotatedEntity( targetEntity, annotatedProperty, context ),
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
			XProperty property,
			ForeignKey foreignKey,
			MetadataBuildingContext context) {
		if ( property.getAnnotation( NotFound.class ) != null ) {
			// supersedes all others
			value.disableForeignKey();
		}
		else {
			final JoinColumn joinColumn = property.getAnnotation( JoinColumn.class );
			final JoinColumns joinColumns = property.getAnnotation( JoinColumns.class );
			final boolean noConstraintByDefault = context.getBuildingOptions().isNoConstraintByDefault();
			if ( joinColumn != null && noConstraint( joinColumn.foreignKey(), noConstraintByDefault )
					|| joinColumns != null && noConstraint( joinColumns.foreignKey(), noConstraintByDefault ) ) {
				value.disableForeignKey();
			}
			else {
				final org.hibernate.annotations.ForeignKey fk =
						property.getAnnotation( org.hibernate.annotations.ForeignKey.class );
				if ( fk != null && isNotEmpty( fk.name() ) ) {
					value.setForeignKeyName( fk.name() );
				}
				else {
					if ( noConstraint( foreignKey, noConstraintByDefault ) ) {
						value.disableForeignKey();
					}
					else if ( foreignKey != null ) {
						value.setForeignKeyName( nullIfEmpty( foreignKey.name() ) );
						value.setForeignKeyDefinition( nullIfEmpty( foreignKey.foreignKeyDefinition() ) );
					}
					else if ( noConstraintByDefault ) {
						value.disableForeignKey();
					}
					else if ( joinColumns != null ) {
						value.setForeignKeyName( nullIfEmpty( joinColumns.foreignKey().name() ) );
						value.setForeignKeyDefinition( nullIfEmpty( joinColumns.foreignKey().foreignKeyDefinition() ) );
					}
					else if ( joinColumn != null ) {
						value.setForeignKeyName( nullIfEmpty( joinColumn.foreignKey().name() ) );
						value.setForeignKeyDefinition( nullIfEmpty( joinColumn.foreignKey().foreignKeyDefinition() ) );
					}
				}
			}
		}
	}

	public static String getReferenceEntityName(PropertyData propertyData, XClass targetEntity, MetadataBuildingContext context) {
		return isDefault( targetEntity, context )
				? propertyData.getClassOrElementName()
				: targetEntity.getName();
	}

	public static String getReferenceEntityName(PropertyData propertyData, MetadataBuildingContext context) {
		final XClass targetEntity = getTargetEntity( propertyData, context );
		return isDefault( targetEntity, context )
				? propertyData.getClassOrElementName()
				: targetEntity.getName();
	}

	public static XClass getTargetEntity(PropertyData propertyData, MetadataBuildingContext context) {
		return context.getBootstrapContext().getReflectionManager()
				.toXClass( getTargetEntityClass( propertyData.getProperty() ) );
	}

	private static Class<?> getTargetEntityClass(XProperty property) {
		final ManyToOne manyToOne = property.getAnnotation( ManyToOne.class );
		if ( manyToOne != null ) {
			return manyToOne.targetEntity();
		}
		final OneToOne oneToOne = property.getAnnotation( OneToOne.class );
		if ( oneToOne != null ) {
			return oneToOne.targetEntity();
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
