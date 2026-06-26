/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.AnnotationException;
import org.hibernate.annotations.FetchProfileOverride;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.PropertyRef;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.mapping.internal.materialize.ResolvedForeignKey;
import org.hibernate.boot.mapping.internal.materialize.ToOneMaterializationHelper;
import org.hibernate.boot.mapping.internal.sources.ColumnSource;
import org.hibernate.boot.mapping.internal.sources.ForeignKeySource;
import org.hibernate.boot.mapping.internal.sources.ToOneSource;
import org.hibernate.boot.mapping.internal.sources.ToOneSource.JoinColumnOrFormulaSource;
import org.hibernate.boot.mapping.internal.view.AttributeBindingView;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.relational.TableReference;
import org.hibernate.boot.mapping.internal.categorize.AttributeMetadata;
import org.hibernate.boot.mapping.internal.categorize.AttributeMetadataImpl;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.IdentifiableTypeMetadata;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SortableValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.FetchType;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;
import jakarta.persistence.SecondaryTable;

/// Binds to-one attributes and association values.
///
/// The immediate work is to create a `ManyToOne` or inverse `OneToOne` mapping
/// value, bind its local columns or join table, and attach it to the owning
/// property.  Order-sensitive pieces are made explicit as typed pending state:
///
/// - non-primary-key targets become [AssociationTargetBinding]
/// - `@MapsId` associations become [DerivedIdentifierBinding]
/// - inverse one-to-one associations become [InverseToOneAssociationBinding]
/// - physical constraints become [ForeignKeyBinding] or [TableForeignKeyBinding]
///
/// This keeps source-level association facts close to the member binder while
/// letting later phases resolve target, identifier, table-key, and constraint
/// details deterministically.
///
/// @since 9.0
/// @author Steve Ebersole
class ToOneAttributeBinder {
	private final IdentifiableTypeMetadata ownerType;
	private final AttributeBindingView attributeBinding;
	private final PersistentClass ownerBinding;
	private final AttributeMetadata attributeMetadata;
	private final Table primaryTable;
	private final ModelBinders modelBinders;
	private final BindingOptions bindingOptions;
	private final BindingState bindingState;
	private final BindingContext bindingContext;

	ToOneAttributeBinder(
			IdentifiableTypeMetadata ownerType,
			AttributeBindingView attributeBinding,
			PersistentClass ownerBinding,
			AttributeMetadata attributeMetadata,
			Table primaryTable,
			ModelBinders modelBinders,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		this.ownerType = ownerType;
		this.attributeBinding = attributeBinding;
		this.ownerBinding = ownerBinding;
		this.attributeMetadata = attributeMetadata;
		this.primaryTable = primaryTable;
		this.modelBinders = modelBinders;
		this.bindingOptions = bindingOptions;
		this.bindingState = bindingState;
		this.bindingContext = bindingContext;
	}

	Value bind(Property property) {
		final MemberDetails member = attributeBinding.member();
		final AssociationOverride associationOverride = attributeBinding.toOneValueIntent().associationOverride();
		final ToOneSource source = ToOneSource.create(
				member,
				ownerType.getClassDetails().getClassName(),
				attributeBinding.attributeName(),
				associationOverride,
				bindingContext.getBootstrapContext().getModelsContext(),
				attributeBinding.resolvedType()
		);
		if ( source.isInverseOneToOne() ) {
			return bindInverseOneToOne( source, property );
		}
		return bindToOne(
				source,
				ownerType,
				ownerBinding,
				ownerType.getClassDetails().getClassName(),
				attributeBinding.attributeName(),
				member,
				property,
				primaryTable,
				modelBinders,
				bindingOptions,
				bindingState,
				bindingContext
		);
	}

	static ManyToOne bindToOne(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			String ownerClassName,
			String propertyName,
			MemberDetails member,
			Property property,
			Table primaryTable,
			AssociationOverride associationOverride,
			ModelBinders modelBinders,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		return bindToOne(
				ownerType,
				ownerBinding,
				ownerClassName,
				propertyName,
				member,
				null,
				property,
				primaryTable,
				associationOverride,
				modelBinders,
				bindingOptions,
				bindingState,
				bindingContext
		);
	}

	static ManyToOne bindToOne(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			String ownerClassName,
			String propertyName,
			MemberDetails member,
			TypeDetails resolvedType,
			Property property,
			Table primaryTable,
			AssociationOverride associationOverride,
			ModelBinders modelBinders,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		return (ManyToOne) bindToOneValue(
				ownerType,
				ownerBinding,
				ownerClassName,
				propertyName,
				member,
				resolvedType,
				property,
				primaryTable,
				associationOverride,
				modelBinders,
				bindingOptions,
				bindingState,
				bindingContext
		);
	}

	static Value bindToOneValue(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			String ownerClassName,
			String propertyName,
			MemberDetails member,
			TypeDetails resolvedType,
			Property property,
			Table primaryTable,
			AssociationOverride associationOverride,
			ModelBinders modelBinders,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		return bindToOneValue(
				ownerType,
				ownerBinding,
				ownerClassName,
				propertyName,
				AttributePath.parse( propertyName ),
				member,
				resolvedType,
				property,
				primaryTable,
				associationOverride,
				modelBinders,
				bindingOptions,
				bindingState,
				bindingContext
		);
	}

	static Value bindToOneValue(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			String ownerClassName,
			String propertyName,
			AttributePath implicitNamingPath,
			MemberDetails member,
			TypeDetails resolvedType,
			Property property,
			Table primaryTable,
			AssociationOverride associationOverride,
			ModelBinders modelBinders,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final ToOneSource source = ToOneSource.create(
				member,
				ownerClassName,
				propertyName,
				implicitNamingPath,
				associationOverride,
				bindingContext.getBootstrapContext().getModelsContext(),
				resolvedType
		);
		if ( source.isInverseOneToOne() ) {
			return bindInverseOneToOne(
					source,
					ownerType,
					ownerBinding,
					new AttributeMetadataImpl( propertyName, AttributeNature.TO_ONE, member ),
					property,
					primaryTable,
					ownerClassName,
					propertyName,
					member,
					bindingOptions,
					bindingState,
					bindingContext
			);
		}
		return bindToOne(
				source,
				ownerType,
				ownerBinding,
				ownerClassName,
				propertyName,
				member,
				property,
				primaryTable,
				modelBinders,
				bindingOptions,
				bindingState,
				bindingContext
		);
	}

	private static ManyToOne bindToOne(
			ToOneSource source,
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			String ownerClassName,
			String propertyName,
			MemberDetails member,
			Property property,
			Table primaryTable,
			ModelBinders modelBinders,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final TargetEntityBinding target = resolveTargetEntityBinding( source, bindingState, bindingContext );
		final JoinTable joinTable = source.joinTable();
		final Table associationTable = joinTable == null
				? resolveAssociationTable( source, primaryTable, bindingState )
				: bindAssociationTable(
						ownerType,
						ownerBinding,
						primaryTable,
						propertyName,
						target,
						joinTable,
						source.notFound() != null,
						modelBinders,
						bindingOptions,
						bindingState,
						bindingContext
				);

		final ManyToOne value = new ManyToOne(
				bindingState.getMetadataBuildingContext(),
				associationTable
		);
		final List<JoinColumnOrFormulaSource> valueJoinColumns = source.valueJoinColumnsOrFormulas(
				joinTable,
				bindingState.getDatabase().getDialect()
		);
			final PropertyRef propertyRef = source.propertyRef();
			final boolean referenceToPrimaryKey = propertyRef == null
					&& referencesPrimaryKeySources( valueJoinColumns, target, bindingState.getDatabase() );
		final MapsId mapsId = member.getDirectAnnotationUsage( MapsId.class );
		value.setReferencedEntityName( target.entityName() );
		value.setReferenceToPrimaryKey( referenceToPrimaryKey );
			value.setTypeName( target.entityName() );
			value.setTypeUsingReflection( ownerClassName, propertyName );
			value.setLazy( effectiveFetchType( source, bindingOptions ) == FetchType.LAZY );
			ToOneMaterializationHelper.applyFetchMode( source, value, ownerBinding );
			applyOnDelete( member, value );
			applyNotFound( source, value );
			applyFetchProfileOverrides( source, ownerBinding, propertyName, bindingState );

		final boolean logicalOneToOne = isLogicalOneToOne( source, joinTable );
		if ( logicalOneToOne ) {
			value.markAsLogicalOneToOne();
		}

		final boolean optional = mapsId == null && source.optional();
		property.setOptional( optional );
		property.setCascade( source.cascades( bindingState ), source.orphanRemoval() );
		final List<Column> sharedIdentifierColumns = resolveSharedIdentifierColumns(
				ownerType,
				associationTable,
				bindingState
		);

		if ( mapsId == null ) {
			final boolean valueColumnsOptional = joinTable == null && optional;
			bindJoinColumns(
					valueJoinColumns,
					value,
					target,
					associationTable,
					referenceToPrimaryKey,
					sharedIdentifierColumns,
					bindingState.getDatabase(),
					logicalOneToOne,
					valueColumnsOptional,
					ownerBinding,
					source,
					ownerClassName + "." + propertyName,
					bindingState
			);
			if ( referenceToPrimaryKey ) {
				value.setSorted( true );
			}
			property.setOptional( optional && value.isNullable() );
		}
		else {
			bindingState.addDerivedIdentifierBinding( new DerivedIdentifierBinding(
					ownerType,
					ownerBinding,
					property,
					value,
					target.typeBinder(),
					referenceToPrimaryKey,
					mapsId.value(),
					source.valueJoinColumns( joinTable ),
					target.identifierColumns(),
					source.valueForeignKeySource( joinTable )
			) );
		}
		if ( propertyRef != null ) {
			UniquePropertyReferenceBinder.bindUniquePropertyReference(
					bindingState,
					value,
					propertyRef.value()
			);
		}
		if ( !referenceToPrimaryKey && propertyRef == null ) {
			bindingState.addAssociationTargetBinding( new AssociationTargetBinding(
					ownerBinding,
					value,
					target.typeBinder(),
					referencedColumnNamesSources( valueJoinColumns ),
					ownerClassName + "." + propertyName
			) );
		}
		if ( mapsId == null && !isUnconstrainedSharedIdentifierOneToOne(
				value,
				logicalOneToOne,
				optional,
				sharedIdentifierColumns,
				bindingState.getDatabase()
		) ) {
			bindingState.addForeignKeyBinding( new ForeignKeyBinding(
					ownerBinding,
					value,
					source.valueForeignKeySource( joinTable ),
					resolvePrimaryKeyForeignKey(
							value,
							target.identifierColumns(),
							referenceToPrimaryKey,
							ownerClassName + "." + propertyName
					),
					referenceToPrimaryKey || propertyRef != null
							? List.of()
							: referencedColumnNamesSources( valueJoinColumns )
			) );
		}
		return value;
	}

	private static boolean isUnconstrainedSharedIdentifierOneToOne(
			ManyToOne value,
			boolean logicalOneToOne,
			boolean optional,
			List<Column> sharedIdentifierColumns,
			Database database) {
		if ( !logicalOneToOne || !optional || sharedIdentifierColumns.isEmpty() ) {
			return false;
		}
		final List<Column> valueColumns = value.getColumns();
		if ( valueColumns.size() != sharedIdentifierColumns.size() ) {
			return false;
		}
		for ( Column column : valueColumns ) {
			if ( !isSharedIdentifierColumn( column, sharedIdentifierColumns, database ) ) {
				return false;
			}
		}
		return true;
	}

	private static boolean isLogicalOneToOne(ToOneSource source, JoinTable joinTable) {
		if ( source.isLogicalOneToOne() ) {
			return true;
		}

		final List<JoinColumn> joinColumns = source.valueJoinColumns( joinTable );
		return joinColumns.size() == 1 && joinColumns.get( 0 ).unique();
	}

	private static ResolvedForeignKey resolvePrimaryKeyForeignKey(
			ManyToOne value,
			List<Column> targetIdentifierColumns,
			boolean referenceToPrimaryKey,
			String sourceRole) {
		if ( !referenceToPrimaryKey || !value.isConstrained() ) {
			return null;
		}
		return ResolvedForeignKey.from(
				value,
				value.getReferencedEntityName(),
				SelectableOrderResolver.resolveByTargetOrder(
						value.getColumns(),
						targetIdentifierColumns,
						sourceRole
				)
		);
	}

	private OneToOne bindInverseOneToOne(ToOneSource source, Property property) {
		return bindInverseOneToOne(
				source,
				ownerType,
				ownerBinding,
				attributeMetadata,
				property,
				primaryTable,
				ownerType.getClassDetails().getClassName(),
				attributeBinding.attributeName(),
				attributeBinding.member(),
				bindingOptions,
				bindingState,
				bindingContext
		);
	}

	private static OneToOne bindInverseOneToOne(
			ToOneSource source,
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			AttributeMetadata attributeMetadata,
			Property property,
			Table primaryTable,
			String ownerClassName,
			String propertyName,
			MemberDetails member,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final TargetEntityBinding target = resolveTargetEntityBinding( source, bindingState, bindingContext );
		final OneToOne value = new OneToOne(
				bindingState.getMetadataBuildingContext(),
				primaryTable,
				ownerBinding
		);
		value.setPropertyName( propertyName );
		value.setReferencedEntityName( target.entityName() );
			value.setTypeName( target.entityName() );
			value.setTypeUsingReflection( ownerClassName, propertyName );
			value.setLazy( effectiveFetchType( source, bindingOptions ) == FetchType.LAZY );
			ToOneMaterializationHelper.applyFetchMode( source, value, ownerBinding );
			value.setConstrained( !source.optional() );
		value.setForeignKeyType( org.hibernate.type.ForeignKeyDirection.TO_PARENT );
		value.setMappedByProperty( source.oneToOne().mappedBy() );
		applyOnDelete( member, value );
		property.setOptional( source.optional() );
		property.setCascade( source.cascades( bindingState ), source.orphanRemoval() );

		bindingState.addInverseToOneAssociationBinding( new InverseToOneAssociationBinding(
				ownerType,
				ownerBinding,
				attributeMetadata,
				property,
				value,
				target.entityNaming().getClassDetails(),
				source.oneToOne().mappedBy()
		) );
		return value;
	}

	private static void applyOnDelete(MemberDetails member, org.hibernate.mapping.SimpleValue value) {
		final OnDelete onDelete = member.getDirectAnnotationUsage( OnDelete.class );
		if ( onDelete != null ) {
			value.setOnDeleteAction( onDelete.action() );
		}
	}

	private static void applyNotFound(ToOneSource source, ManyToOne value) {
		final NotFound notFound = source.notFound();
		if ( notFound != null ) {
			value.setNotFoundAction( notFound.action() );
		}
	}

	private static void applyFetchProfileOverrides(
			ToOneSource source,
			PersistentClass ownerBinding,
			String propertyName,
			BindingState bindingState) {
		for ( FetchProfileOverride override : source.fetchProfileOverrides() ) {
			final FetchProfile profile = bindingState.getFetchProfile( override.profile() );
			if ( profile == null ) {
				throw new AnnotationException( "Property '" + ownerBinding.getEntityName() + "." + propertyName
						+ "' refers to an unknown fetch profile named '" + override.profile() + "'" );
			}
			profile.addFetch( new FetchProfile.Fetch(
					ownerBinding.getEntityName(),
					propertyName,
					override.mode(),
					override.fetch()
			) );
		}
	}

	private static void bindJoinColumns(
			List<JoinColumnOrFormulaSource> joinColumnAnns,
			ManyToOne value,
			TargetEntityBinding target,
			Table table,
			boolean referenceToPrimaryKey,
			List<Column> sharedIdentifierColumns,
			Database database,
			boolean uniqueByDefault,
			boolean optional,
			PersistentClass ownerBinding,
			ToOneSource source,
			String sourceRole,
			BindingState bindingState) {
		final List<Column> targetColumns = referenceToPrimaryKey
				? referencedPrimaryKeyColumns( joinColumnAnns, target, database )
				: target.identifierColumns();

		if ( referenceToPrimaryKey && !joinColumnAnns.isEmpty() && joinColumnAnns.size() != targetColumns.size() ) {
			throw new MappingException(
					"Composite to-one join column count did not match target identifier column count - "
							+ sourceRole
			);
		}

		final List<JoinColumnOrFormulaSource> orderedJoinColumns = referenceToPrimaryKey
				? orderJoinColumnSources(
						joinColumnAnns,
						targetColumns,
						database,
						source.ownerClassName(),
						source.propertyName()
				)
				: joinColumnAnns;
		final int columnCount = referenceToPrimaryKey ? targetColumns.size() : joinColumnAnns.size();
		for ( int i = 0; i < columnCount; i++ ) {
			final JoinColumnOrFormulaSource joinColumnAnn = orderedJoinColumns.isEmpty() ? null : orderedJoinColumns.get( i );
			final String targetColumnName = referenceToPrimaryKey
					? targetColumns.get( i ).getName()
					: joinColumnAnn.referencedColumnName();
			if ( joinColumnAnn != null && joinColumnAnn.formula() != null ) {
				value.addFormula( new Formula( joinColumnAnn.formula().value() ) );
				continue;
			}
			final Column column = ColumnBinder.bindColumn(
					ColumnSource.from( joinColumnAnn == null ? null : joinColumnAnn.column() ),
					() -> implicitJoinColumnName( ownerBinding, source, target, targetColumnName, database, bindingState ),
					uniqueByDefault,
					optional
			);
			if ( joinColumnAnn == null || joinColumnAnn.column() == null ) {
				final boolean nullable = column.isNullable();
				column.copy( targetColumns.get( i ) );
				column.setNullable( nullable );
			}
			if ( uniqueByDefault ) {
				column.setUnique( true );
			}
			if ( !optional ) {
				column.setNullable( false );
			}
			table.addColumn( column );
			final boolean sharedIdentifierColumn = isSharedIdentifierColumn( column, sharedIdentifierColumns, database );
			value.addColumn(
					column,
					!sharedIdentifierColumn
							&& ( joinColumnAnn == null || joinColumnAnn.column() == null || joinColumnAnn.column().insertable() ),
					!sharedIdentifierColumn
							&& ( joinColumnAnn == null || joinColumnAnn.column() == null || joinColumnAnn.column().updatable() )
			);
		}
	}

	private static String implicitJoinColumnName(
			PersistentClass ownerBinding,
			ToOneSource source,
			TargetEntityBinding target,
			String targetColumnName,
			Database database,
			BindingState bindingState) {
		return bindingState.getMetadataBuildingContext()
				.getBuildingOptions()
				.getImplicitNamingStrategy()
				.determineJoinColumnName( new ImplicitJoinColumnNameSource() {
					@Override
					public Nature getNature() {
						return Nature.ENTITY;
					}

					@Override
					public EntityNaming getEntityNaming() {
						return new EntityNaming() {
							@Override
							public String getClassName() {
								return ownerBinding.getClassName();
							}

							@Override
							public String getEntityName() {
								return ownerBinding.getEntityName();
							}

							@Override
							public String getJpaEntityName() {
								return ownerBinding.getJpaEntityName();
							}
						};
					}

					@Override
					public AttributePath getAttributePath() {
						return source.implicitNamingPath();
					}

					@Override
					public Identifier getReferencedTableName() {
						return target.primaryTable().getNameIdentifier();
					}

					@Override
					public Identifier getReferencedColumnName() {
						return database.toIdentifier( targetColumnName );
					}

					@Override
					public org.hibernate.boot.spi.MetadataBuildingContext getBuildingContext() {
						return bindingState.getMetadataBuildingContext();
					}
				} )
				.getText();
	}

	private static List<Column> resolveSharedIdentifierColumns(
			IdentifiableTypeMetadata ownerType,
			Table associationTable,
			BindingState bindingState) {
		final IdentifierBinding entityIdentifierBinding = bindingState.getIdentifierBinding( ownerType.getHierarchy().getRoot() );
		if ( entityIdentifierBinding == null || entityIdentifierBinding.table() != associationTable ) {
			return List.of();
		}
		return entityIdentifierBinding.columns();
	}

	private static boolean isSharedIdentifierColumn(Column column, List<Column> identifierColumns, Database database) {
		for ( Column identifierColumn : identifierColumns ) {
			if ( column.getNameIdentifier( database ).matches( identifierColumn.getNameIdentifier( database ) ) ) {
				return true;
			}
		}
		return false;
	}

	static boolean referencesPrimaryKey(List<JoinColumn> joinColumns, List<Column> targetColumns, Database database) {
		if ( joinColumns.isEmpty()
				|| joinColumns.stream().noneMatch( (joinColumn) -> StringHelper.isNotEmpty( joinColumn.referencedColumnName() ) ) ) {
			return true;
		}
		if ( joinColumns.size() != targetColumns.size() ) {
			return false;
		}
		final ArrayList<Column> unmatchedTargetColumns = new ArrayList<>( targetColumns );
		for ( JoinColumn joinColumn : joinColumns ) {
			final Column targetColumn = findTargetColumn( unmatchedTargetColumns, joinColumn.referencedColumnName(), database );
			if ( targetColumn == null ) {
				return false;
			}
			unmatchedTargetColumns.remove( targetColumn );
		}
		return unmatchedTargetColumns.isEmpty();
	}

	private static boolean referencesPrimaryKeySources(
			List<JoinColumnOrFormulaSource> joinColumns,
			TargetEntityBinding target,
			Database database) {
		return referencesPrimaryKeySources( joinColumns, target.identifierColumns(), database )
			|| referencesSecondaryTablePrimaryKeySources( joinColumns, target, database );
	}

	private static boolean referencesPrimaryKeySources(
			List<JoinColumnOrFormulaSource> joinColumns,
			List<Column> targetColumns,
			Database database) {
		if ( joinColumns.isEmpty()
				|| joinColumns.stream().noneMatch( (joinColumn) -> StringHelper.isNotEmpty( joinColumn.referencedColumnName() ) ) ) {
			return true;
		}
		if ( joinColumns.size() != targetColumns.size() ) {
			return false;
		}
		final ArrayList<Column> unmatchedTargetColumns = new ArrayList<>( targetColumns );
		for ( JoinColumnOrFormulaSource joinColumn : joinColumns ) {
			final Column targetColumn = findTargetColumn( unmatchedTargetColumns, joinColumn.referencedColumnName(), database );
			if ( targetColumn == null ) {
				return false;
			}
			unmatchedTargetColumns.remove( targetColumn );
		}
		return unmatchedTargetColumns.isEmpty();
	}

	private static List<Column> referencedPrimaryKeyColumns(
			List<JoinColumnOrFormulaSource> joinColumns,
			TargetEntityBinding target,
			Database database) {
		if ( referencesPrimaryKeySources( joinColumns, target.identifierColumns(), database ) ) {
			return target.identifierColumns();
		}
		final PrimaryKeyJoinColumn[] primaryKeyJoinColumns = targetPrimaryKeyJoinColumns( target );
		if ( referencesPrimaryKeyJoinColumns( joinColumns, primaryKeyJoinColumns, database ) ) {
			return primaryKeyJoinColumns( primaryKeyJoinColumns );
		}
		final SecondaryTable[] secondaryTables = target.typeBinder()
				.getManagedType()
				.getClassDetails()
				.getRepeatedAnnotationUsages(
						SecondaryTable.class,
						target.typeBinder().getBindingContext().getBootstrapContext().getModelsContext()
				);
		for ( SecondaryTable secondaryTable : secondaryTables ) {
			if ( referencesPrimaryKeyJoinColumns( joinColumns, secondaryTable.pkJoinColumns(), database ) ) {
				return primaryKeyJoinColumns( secondaryTable.pkJoinColumns() );
			}
		}
		return target.identifierColumns();
	}

	private static List<Column> primaryKeyJoinColumns(PrimaryKeyJoinColumn[] primaryKeyJoinColumns) {
		final ArrayList<Column> result = new ArrayList<>( primaryKeyJoinColumns.length );
		for ( PrimaryKeyJoinColumn primaryKeyJoinColumn : primaryKeyJoinColumns ) {
			result.add( new Column( primaryKeyJoinColumn.name() ) );
		}
		return result;
	}

	private static boolean referencesSecondaryTablePrimaryKeySources(
			List<JoinColumnOrFormulaSource> joinColumns,
			TargetEntityBinding target,
			Database database) {
		if ( joinColumns.isEmpty()
				|| joinColumns.stream().noneMatch( (joinColumn) -> StringHelper.isNotEmpty( joinColumn.referencedColumnName() ) ) ) {
			return false;
		}
		if ( referencesPrimaryKeyJoinColumns( joinColumns, targetPrimaryKeyJoinColumns( target ), database ) ) {
			return true;
		}
		final SecondaryTable[] secondaryTables = target.typeBinder()
				.getManagedType()
				.getClassDetails()
				.getRepeatedAnnotationUsages(
						SecondaryTable.class,
						target.typeBinder().getBindingContext().getBootstrapContext().getModelsContext()
				);
		for ( SecondaryTable secondaryTable : secondaryTables ) {
			if ( referencesPrimaryKeyJoinColumns( joinColumns, secondaryTable.pkJoinColumns(), database ) ) {
				return true;
			}
		}
		return false;
	}

	private static PrimaryKeyJoinColumn[] targetPrimaryKeyJoinColumns(TargetEntityBinding target) {
		final ClassDetails classDetails = target.typeBinder().getManagedType().getClassDetails();
		final PrimaryKeyJoinColumns primaryKeyJoinColumns = classDetails.getDirectAnnotationUsage(
				PrimaryKeyJoinColumns.class
		);
		if ( primaryKeyJoinColumns != null ) {
			return primaryKeyJoinColumns.value();
		}
		return classDetails.getRepeatedAnnotationUsages(
				PrimaryKeyJoinColumn.class,
				target.typeBinder().getBindingContext().getBootstrapContext().getModelsContext()
		);
	}

	private static boolean referencesPrimaryKeyJoinColumns(
			List<JoinColumnOrFormulaSource> joinColumns,
			PrimaryKeyJoinColumn[] primaryKeyJoinColumns,
			Database database) {
		if ( joinColumns.size() != primaryKeyJoinColumns.length ) {
			return false;
		}
		final ArrayList<PrimaryKeyJoinColumn> unmatchedPrimaryKeyJoinColumns = new ArrayList<>(
				List.of( primaryKeyJoinColumns )
		);
		for ( JoinColumnOrFormulaSource joinColumn : joinColumns ) {
			final PrimaryKeyJoinColumn primaryKeyJoinColumn = findPrimaryKeyJoinColumn(
					unmatchedPrimaryKeyJoinColumns,
					joinColumn.referencedColumnName(),
					database
			);
			if ( primaryKeyJoinColumn == null ) {
				return false;
			}
			unmatchedPrimaryKeyJoinColumns.remove( primaryKeyJoinColumn );
		}
		return unmatchedPrimaryKeyJoinColumns.isEmpty();
	}

	private static PrimaryKeyJoinColumn findPrimaryKeyJoinColumn(
			List<PrimaryKeyJoinColumn> primaryKeyJoinColumns,
			String columnName,
			Database database) {
		final Identifier columnIdentifier = database.toIdentifier( columnName );
		for ( PrimaryKeyJoinColumn primaryKeyJoinColumn : primaryKeyJoinColumns ) {
			if ( database.toIdentifier( primaryKeyJoinColumn.name() ).matches( columnIdentifier ) ) {
				return primaryKeyJoinColumn;
			}
		}
		return null;
	}

	static List<String> referencedColumnNames(List<JoinColumn> joinColumns) {
		final ArrayList<String> result = new ArrayList<>( joinColumns.size() );
		for ( JoinColumn joinColumn : joinColumns ) {
			result.add( joinColumn.referencedColumnName() );
		}
		return result;
	}

	private static List<String> referencedColumnNamesSources(List<JoinColumnOrFormulaSource> joinColumns) {
		final ArrayList<String> result = new ArrayList<>( joinColumns.size() );
		for ( JoinColumnOrFormulaSource joinColumn : joinColumns ) {
			result.add( joinColumn.referencedColumnName() );
		}
		return result;
	}

	private static Column findTargetColumn(List<Column> targetColumns, String columnName, Database database) {
		final Identifier columnIdentifier = database.toIdentifier( columnName );
		for ( Column targetColumn : targetColumns ) {
			if ( targetColumn.getNameIdentifier( database ).matches( columnIdentifier ) ) {
				return targetColumn;
			}
		}
		return null;
	}

	static List<JoinColumn> orderJoinColumns(
			List<JoinColumn> joinColumns,
			List<Column> targetColumns,
			Database database,
			String ownerClassName,
			String propertyName) {
		if ( joinColumns.isEmpty() || joinColumns.stream().noneMatch( (joinColumn) -> StringHelper.isNotEmpty( joinColumn.referencedColumnName() ) ) ) {
			return joinColumns;
		}

		final ArrayList<JoinColumn> orderedJoinColumns = new ArrayList<>( targetColumns.size() );
		final ArrayList<JoinColumn> unmatchedJoinColumns = new ArrayList<>( joinColumns );
		for ( Column targetColumn : targetColumns ) {
			final JoinColumn joinColumn = findJoinColumn(
					targetColumn,
					unmatchedJoinColumns,
					database,
					ownerClassName,
					propertyName
			);
			orderedJoinColumns.add( joinColumn );
			unmatchedJoinColumns.remove( joinColumn );
		}
		return orderedJoinColumns;
	}

	static List<JoinColumnOrFormulaSource> orderJoinColumnSources(
			List<JoinColumnOrFormulaSource> joinColumns,
			List<Column> targetColumns,
			Database database,
			String ownerClassName,
			String propertyName) {
		if ( joinColumns.isEmpty() || joinColumns.stream().noneMatch( (joinColumn) -> StringHelper.isNotEmpty( joinColumn.referencedColumnName() ) ) ) {
			return joinColumns;
		}

		final ArrayList<JoinColumnOrFormulaSource> orderedJoinColumns = new ArrayList<>( targetColumns.size() );
		final ArrayList<JoinColumnOrFormulaSource> unmatchedJoinColumns = new ArrayList<>( joinColumns );
		for ( Column targetColumn : targetColumns ) {
			final JoinColumnOrFormulaSource joinColumn = findJoinColumnSource(
					targetColumn,
					unmatchedJoinColumns,
					database,
					ownerClassName,
					propertyName
			);
			orderedJoinColumns.add( joinColumn );
			unmatchedJoinColumns.remove( joinColumn );
		}
		return orderedJoinColumns;
	}

	private static JoinColumn findJoinColumn(
			Column targetColumn,
			List<JoinColumn> joinColumns,
			Database database,
			String ownerClassName,
			String propertyName) {
		for ( JoinColumn joinColumn : joinColumns ) {
			if ( targetColumn.getNameIdentifier( database ).matches( database.toIdentifier( joinColumn.referencedColumnName() ) ) ) {
				return joinColumn;
			}
		}

		throw new MappingException(
				"Unable to match join column referencedColumnName to target identifier column `"
						+ targetColumn.getName() + "` - " + ownerClassName + "." + propertyName
		);
	}

	private static JoinColumnOrFormulaSource findJoinColumnSource(
			Column targetColumn,
			List<JoinColumnOrFormulaSource> joinColumns,
			Database database,
			String ownerClassName,
			String propertyName) {
		for ( JoinColumnOrFormulaSource joinColumn : joinColumns ) {
			if ( targetColumn.getNameIdentifier( database ).matches( database.toIdentifier( joinColumn.referencedColumnName() ) ) ) {
				return joinColumn;
			}
		}

		throw new MappingException(
				"Unable to match join column referencedColumnName to target identifier column `"
						+ targetColumn.getName() + "` - " + ownerClassName + "." + propertyName
		);
	}

	private static Table bindAssociationTable(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			Table primaryTable,
			String propertyName,
			TargetEntityBinding target,
			JoinTable joinTable,
			boolean disableForeignKeyCreation,
			ModelBinders modelBinders,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		if ( ownerBinding == null ) {
			throw new UnsupportedOperationException( "To-one @JoinTable requires an owning PersistentClass" );
		}
		final Table associationTable = modelBinders.getTableBinder()
				.bindAssociationTable(
						resolveOwnerEntityType( ownerType ),
						primaryTable,
						propertyName,
						target.entityNaming(),
						target.primaryTable(),
						joinTable
				)
				.binding();

		final Join join = new Join();
		join.setTable( associationTable );
		join.setPersistentClass( ownerBinding );
		join.setOptional( true );
		join.setInverse( false );
		if ( disableForeignKeyCreation ) {
			join.disableForeignKeyCreation();
		}
		ownerBinding.addJoin( join );

		final IdentifierBinding ownerIdentifierBinding = bindingState.getIdentifierBinding( ownerType.getHierarchy().getRoot() );
		if ( ownerIdentifierBinding == null ) {
			throw new MappingException(
					"Could not resolve identifier binding for to-one association table owner - "
							+ ownerType.getClassDetails().getClassName()
			);
		}
		final List<JoinColumn> joinColumns = listJoinColumns( joinTable.joinColumns() );
		if ( !joinColumns.isEmpty()
				&& joinColumns.size() != ownerIdentifierBinding.columns().size()
				&& !hasReferencedColumnName( joinColumns ) ) {
			throw new MappingException(
					"Association table join column count did not match owner identifier column count - "
							+ ownerType.getClassDetails().getClassName()
			);
		}
		bindingState.addAssociationTableBinding( new AssociationTableBinding(
				join,
				joinColumns,
				disableForeignKeyCreation
						? ForeignKeySource.noConstraint()
						: ForeignKeySource.firstSpecified(
								ForeignKeySource.fromFirstSpecifiedJoinColumn( joinColumns ),
								ForeignKeySource.from( joinTable )
						)
		) );
		return associationTable;
	}

	private static Table resolveAssociationTable(
			ToOneSource source,
			Table primaryTable,
			BindingState bindingState) {
		final List<JoinColumn> joinColumns = source.joinColumns();
		final String tableName = resolveJoinTableName( joinColumns, primaryTable );
		if ( StringHelper.isEmpty( tableName ) ) {
			return primaryTable;
		}

		final Identifier identifier = Identifier.toIdentifier( tableName );
		final TableReference tableByName = bindingState.getTableByName( identifier.getCanonicalName() );
		return tableByName.binding();
	}

	private static String resolveJoinTableName(List<JoinColumn> joinColumns, Table primaryTable) {
		String tableName = null;
		for ( JoinColumn joinColumn : joinColumns ) {
			final String joinColumnTableName = StringHelper.isEmpty( joinColumn.table() )
					? primaryTable.getName()
					: joinColumn.table();
			if ( tableName != null && !tableName.equals( joinColumnTableName ) ) {
				throw new MappingException( "To-one join columns cannot span multiple tables" );
			}
			tableName = joinColumnTableName;
		}
		return primaryTable.getName().equals( tableName ) ? null : tableName;
	}

	static List<JoinColumn> resolveJoinColumns(
			MemberDetails member,
			AssociationOverride associationOverride) {
		return ToOneSource.create( member, "", "", associationOverride, null ).joinColumns();
	}

	static boolean hasReferencedColumnName(List<JoinColumn> joinColumns) {
		return joinColumns.stream().anyMatch( (joinColumn) -> StringHelper.isNotEmpty( joinColumn.referencedColumnName() ) );
	}

	private static List<JoinColumn> listJoinColumns(JoinColumn[] joinColumns) {
		if ( joinColumns.length == 0 ) {
			return List.of();
		}
		final ArrayList<JoinColumn> result = new ArrayList<>( joinColumns.length );
		for ( JoinColumn joinColumn : joinColumns ) {
			result.add( joinColumn );
		}
		return result;
	}

	private static TargetEntityBinding resolveTargetEntityBinding(
			ToOneSource source,
			BindingState bindingState,
			BindingContext bindingContext) {
		final ClassDetails targetClassDetails = source.targetClassDetails( bindingContext );
		final EntityTypeBinder targetTypeBinder = (EntityTypeBinder) bindingState.getTypeBinder(
				targetClassDetails
		);
		if ( targetTypeBinder == null ) {
			throw new MappingException(
					"Could not resolve local type binding for to-one target entity - "
							+ targetClassDetails.getClassName()
			);
		}

		final IdentifierBinding entityIdentifierBinding = bindingState.getIdentifierBinding(
				targetTypeBinder.getManagedType().getHierarchy().getRoot()
		);
		if ( entityIdentifierBinding == null ) {
			throw new MappingException(
					"Could not resolve identifier binding for to-one target entity - "
							+ targetTypeBinder.getTypeBinding().getEntityName()
			);
		}

		return new TargetEntityBinding(
				targetTypeBinder.getTypeBinding().getEntityName(),
				targetTypeBinder,
				targetTypeBinder.getManagedType(),
				entityIdentifierBinding.table(),
				entityIdentifierBinding,
				entityIdentifierBinding.columns()
		);
	}

	private static EntityTypeMetadata resolveOwnerEntityType(IdentifiableTypeMetadata ownerType) {
		if ( ownerType instanceof EntityTypeMetadata entityType ) {
			return entityType;
		}
		return ownerType.getHierarchy().getRoot();
	}

	private static FetchType effectiveFetchType(ToOneSource source, BindingOptions bindingOptions) {
		return source.effectiveFetchType( bindingOptions.getDefaultToOneFetchType() );
	}

	private record TargetEntityBinding(
			String entityName,
			EntityTypeBinder typeBinder,
			EntityTypeMetadata entityNaming,
			Table primaryTable,
			IdentifierBinding entityIdentifierBinding,
			List<Column> identifierColumns) {
		@Override
		public List<Column> identifierColumns() {
			if ( entityIdentifierBinding.value() instanceof SortableValue sortableValue ) {
				sortableValue.sortProperties();
				return entityIdentifierBinding.value().getColumns();
			}
			if ( primaryTable.getPrimaryKey() != null
					&& !primaryTable.getPrimaryKey().getColumns().isEmpty()
					&& primaryTable.getPrimaryKey().getColumns().size() >= identifierColumns.size() ) {
				return primaryTable.getPrimaryKey().getColumns();
			}
			return identifierColumns;
		}
	}
}
