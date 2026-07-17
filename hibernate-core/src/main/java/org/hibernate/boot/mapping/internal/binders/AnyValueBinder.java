/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.annotations.AnyDiscriminatorImplicitValues;
import org.hibernate.annotations.AnyKeyJavaType;
import org.hibernate.boot.internal.AnyKeyType;
import org.hibernate.boot.model.naming.ImplicitAnyDiscriminatorColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitAnyKeyColumnNameSource;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.mapping.internal.categorize.BasicKeyMapping;
import org.hibernate.boot.mapping.internal.sources.AnySource;
import org.hibernate.boot.mapping.internal.sources.BasicValueSource;
import org.hibernate.boot.mapping.internal.sources.ColumnSource;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.internal.FullNameImplicitDiscriminatorStrategy;
import org.hibernate.metamodel.internal.ShortNameImplicitDiscriminatorStrategy;
import org.hibernate.metamodel.mapping.DiscriminatorValue;
import org.hibernate.metamodel.spi.ImplicitDiscriminatorStrategy;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.boot.spi.MetadataBuildingContext;

import jakarta.persistence.DiscriminatorType;

/// Binds the shared mapping value used by singular `@Any` and plural `@ManyToAny`.
///
/// The value is physically two basic values on the owning table: a discriminator
/// and an entity-id key.  The key may span multiple columns, but still has one
/// explicit key Java type for this prototype.
///
/// Supported mapping controls:
///
/// - `@Any(fetch = ...)` and `@ManyToAny(fetch = ...)` drive [Any#setLazy(boolean)].
/// - Singular `@Any(optional = ...)` drives discriminator/key column nullability
///   defaults and the property optional flag in [AnyAttributeBinder].
/// - `@Column` names and configures the discriminator column.
/// - `@Formula` configures formula-based discriminator storage.
/// - `@AnyDiscriminator` chooses the discriminator Java type: `STRING`,
///   `INTEGER`, or `CHAR`.
/// - `@JdbcType` and `@JdbcTypeCode` are applied by [BasicValueSourceBinder] to the
///   discriminator value.
/// - `@AnyDiscriminatorValue(s)` creates explicit discriminator-to-entity
///   mappings.
/// - `@AnyDiscriminatorImplicitValues` sets the mapping model's implicit
///   discriminator strategy; otherwise the binder defaults to full entity names.
/// - `@AnyKeyJavaClass` supplies the key Java type.  When absent, explicit
///   discriminator target entities may infer the key Java type from matching
///   basic target identifiers.
/// - `@AnyKeyJavaType`, `@AnyKeyJdbcType`, and `@AnyKeyJdbcTypeCode` are applied
///   by [BasicValueSourceBinder] to the key value.
/// - Singular `@JoinColumn`, singular `@JoinTable#inverseJoinColumns`, or plural
///   `@JoinTable#inverseJoinColumns` names the key column or columns.  If absent,
///   the binder uses the current implicit key-column default for this prototype.
///
/// @since 9.0
/// @author Steve Ebersole
class AnyValueBinder {
	private final BindingOptions bindingOptions;
	private final BindingState bindingState;
	private final BindingContext bindingContext;

	AnyValueBinder(
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		this.bindingOptions = bindingOptions;
		this.bindingState = bindingState;
		this.bindingContext = bindingContext;
	}

	Any bind(AnySource source, String propertyName, Table table) {
		validateSupportedShape( source, propertyName );

		final Any any = new Any( bindingState.getMetadataBuildingContext(), table, true );
		any.setLazy( source.lazy() );
		any.setDiscriminator( bindDiscriminator( source, propertyName, table ) );
		any.setDiscriminatorValueMappings( bindDiscriminatorValueMappings( source, propertyName ) );
		any.setImplicitDiscriminatorValueStrategy( resolveImplicitDiscriminatorStrategy( source ) );
		final BasicValue key = bindKey( source, propertyName, table );
		any.setKey( key );
		addAdditionalKeySelectables( any, key );
		any.setTypeUsingReflection(
				source.member().getDeclaringType().getName(),
				propertyName,
				bindingState.getMetadataBuildingContext()
		);
		return any;
	}

	private BasicValue bindDiscriminator(AnySource source, String propertyName, Table table) {
		final BasicValue discriminator = BasicValue.unregistered( bindingState.getMetadataBuildingContext(), table );
		discriminator.setTable( table );

		if ( source.discriminatorFormula() != null ) {
			discriminator.addFormula( new Formula( source.discriminatorFormula().value() ) );
		}
		else {
			final var columnSource = ColumnSource.from( source.discriminatorColumn() );
			final Column column = ColumnBinder.bindColumn(
					columnSource,
					() -> implicitAnyDiscriminatorColumnName( source ),
					false,
					source.effectiveOptional(),
					defaultDiscriminatorLength( source.discriminatorType() ),
					0,
					0
			);
			table.addColumn( column );
			discriminator.addColumn(
					column,
					columnSource == null || columnSource.insertable( true ),
					columnSource == null || columnSource.updatable( true )
			);
		}

		final var resolutionInput = BasicValueSourceBinder.bindBasicValue(
				BasicValueSource.anyDiscriminator( source.member(), source.discriminatorJavaType() ),
				null,
				discriminator,
				bindingOptions,
				bindingState,
				bindingContext
		);
			bindingState.addAttributeValueResolution( AttributeBindingPhase.valueResolution(
					resolutionInput,
					bindingState.getMetadataBuildingContext().getServiceComponents(),
					bindingState.getMappingResolutionState(),
					bindingState.getMetadataBuildingContext()
			) );
		return discriminator;
	}

	private BasicValue bindKey(AnySource source, String propertyName, Table table) {
		final BasicValue key = BasicValue.unregistered( bindingState.getMetadataBuildingContext(), table );
		key.setTable( table );

		if ( source.keyColumns().isEmpty() ) {
			final Column column = ColumnBinder.bindColumn(
					null,
					() -> implicitAnyKeyColumnName( source ),
					false,
					source.effectiveOptional()
			);
			table.addColumn( column );
			key.addColumn( column, true, true );
		}
		else {
			for ( int i = 0; i < source.keyColumns().size(); i++ ) {
				final int index = i;
				final var columnSource = ColumnSource.from( source.keyColumns().get( index ) );
				final Column column = ColumnBinder.bindColumn(
						columnSource,
						() -> index == 0
								? implicitAnyKeyColumnName( source )
								: implicitAnyKeyColumnName( source ) + ( index + 1 ),
						false,
						source.effectiveOptional()
				);
				table.addColumn( column );
				key.addColumn(
						column,
						columnSource == null || columnSource.insertable( true ),
						columnSource == null || columnSource.updatable( true )
				);
			}
		}

		final var resolutionInput = BasicValueSourceBinder.bindBasicValue(
				BasicValueSource.anyKey( source.member(), resolveKeyJavaType( source ) ),
				null,
				key,
				bindingOptions,
				bindingState,
				bindingContext
		);
			bindingState.addAttributeValueResolution( AttributeBindingPhase.valueResolution(
					resolutionInput,
					bindingState.getMetadataBuildingContext().getServiceComponents(),
					bindingState.getMappingResolutionState(),
					bindingState.getMetadataBuildingContext()
			) );
		return key;
	}

	private String implicitAnyDiscriminatorColumnName(AnySource source) {
		return bindingContext.getImplicitNamingStrategy()
				.determineAnyDiscriminatorColumnName( new ImplicitAnyDiscriminatorColumnNameSource() {
					@Override
					public AttributePath getAttributePath() {
						return AttributePath.parse( source.member().resolveAttributeName() );
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						return bindingState.getMetadataBuildingContext();
					}
				} )
				.getText();
	}

	private String implicitAnyKeyColumnName(AnySource source) {
		return bindingContext.getImplicitNamingStrategy()
				.determineAnyKeyColumnName( new ImplicitAnyKeyColumnNameSource() {
					@Override
					public AttributePath getAttributePath() {
						return AttributePath.parse( source.member().resolveAttributeName() );
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						return bindingState.getMetadataBuildingContext();
					}
				} )
				.getText();
	}

	private void addAdditionalKeySelectables(Any any, BasicValue key) {
		final java.util.List<Selectable> selectables = key.getSelectables();
		for ( int i = 1; i < selectables.size(); i++ ) {
			any.addSelectable( selectables.get( i ) );
		}
	}

	private Map<DiscriminatorValue, Class<?>> bindDiscriminatorValueMappings(
			AnySource source,
			String propertyName) {
		final Map<DiscriminatorValue, Class<?>> result = new HashMap<>();
		for ( org.hibernate.annotations.AnyDiscriminatorValue discriminatorValue : source.discriminatorValues() ) {
			final Class<?> entityClass = discriminatorValue.entity();
			resolveTargetTypeBinder( entityClass, source.member() );
			result.put(
					DiscriminatorValue.of( parseDiscriminatorValue(
							discriminatorValue.discriminator(),
							source.discriminatorType(),
							source.member()
					) ),
					entityClass
			);
		}
		return result;
	}

	private void validateSupportedShape(AnySource source, String propertyName) {
		if ( source.discriminatorColumn() != null && source.discriminatorFormula() != null ) {
			throw new MappingException(
					"@Any discriminator cannot be mapped with both @Column and @Formula - "
							+ source.member().getName()
			);
		}
		if ( !hasExplicitKeyType( source ) && source.discriminatorValues().isEmpty() ) {
			throw new UnsupportedOperationException(
					"@Any requires explicit key type metadata when no explicit discriminator target mappings are available for key type inference - "
							+ source.member().getName()
			);
		}
	}

	private boolean hasExplicitKeyType(AnySource source) {
		final var modelsContext = bindingContext.getModelsContext();
		return source.keyJavaClass() != null
				|| source.member().locateAnnotationUsage( AnyKeyJavaType.class, modelsContext ) != null
				|| source.member().locateAnnotationUsage( AnyKeyType.class, modelsContext ) != null;
	}

	private ImplicitDiscriminatorStrategy resolveImplicitDiscriminatorStrategy(AnySource source) {
		final AnyDiscriminatorImplicitValues implicitValues = source.implicitDiscriminatorValues();
		if ( implicitValues == null ) {
			return null;
		}
		return switch ( implicitValues.value() ) {
			case FULL_NAME -> FullNameImplicitDiscriminatorStrategy.FULL_NAME_STRATEGY;
			case SHORT_NAME -> ShortNameImplicitDiscriminatorStrategy.SHORT_NAME_STRATEGY;
			case CUSTOM -> customImplicitDiscriminatorStrategy( implicitValues );
		};
	}

	private ImplicitDiscriminatorStrategy customImplicitDiscriminatorStrategy(
			AnyDiscriminatorImplicitValues implicitValues) {
		final Class<? extends ImplicitDiscriminatorStrategy> implementation = implicitValues.implementation();
		if ( implementation == ImplicitDiscriminatorStrategy.class ) {
			return null;
		}
		return bindingContext.getCustomTypeProducer()
				.produceBeanInstance( implementation );
	}

	private Class<?> resolveKeyJavaType(AnySource source) {
		if ( source.keyJavaClass() != null ) {
			return source.keyJavaClass();
		}

		Class<?> inferredKeyType = null;
		for ( org.hibernate.annotations.AnyDiscriminatorValue discriminatorValue : source.discriminatorValues() ) {
			final EntityTypeBinder targetTypeBinder = resolveTargetTypeBinder( discriminatorValue.entity(), source.member() );
			final IdentifierBinding entityIdentifierBinding = bindingState.getIdentifierBinding(
					targetTypeBinder.getManagedType().getHierarchy().getRoot()
			);
			if ( entityIdentifierBinding == null ) {
				throw new MappingException(
						"Could not resolve identifier binding for @Any target entity `"
								+ targetTypeBinder.getTypeBinding().getEntityName() + "` - " + source.member().getName()
				);
			}
			if ( !( entityIdentifierBinding.keyMapping() instanceof BasicKeyMapping basicKeyMapping ) ) {
				throw new UnsupportedOperationException(
						"@Any key Java type inference requires basic target identifiers - "
								+ targetTypeBinder.getTypeBinding().getEntityName() + " - " + source.member().getName()
				);
			}

			final Class<?> targetKeyType = basicKeyMapping.getKeyType().toJavaClass();
			if ( inferredKeyType == null ) {
				inferredKeyType = targetKeyType;
			}
			else if ( !inferredKeyType.equals( targetKeyType ) ) {
				throw new MappingException(
						"@Any target identifier Java types did not match for key type inference - "
								+ inferredKeyType.getName() + " and " + targetKeyType.getName()
								+ " - " + source.member().getName()
				);
			}
		}

		return inferredKeyType;
	}

	private EntityTypeBinder resolveTargetTypeBinder(Class<?> entityClass, MemberDetails member) {
		final ClassDetails entityClassDetails = bindingContext.getClassDetailsRegistry()
				.resolveClassDetails( entityClass.getName() );
		final EntityTypeBinder targetTypeBinder = (EntityTypeBinder) bindingState.getTypeBinder( entityClassDetails );
		if ( targetTypeBinder == null ) {
			throw new MappingException(
					"@Any discriminator value referenced an unknown entity `"
							+ entityClass.getName() + "` - " + member.getName()
			);
		}
		return targetTypeBinder;
	}

	private Object parseDiscriminatorValue(
			String value,
			DiscriminatorType discriminatorType,
			MemberDetails member) {
		try {
			return switch ( discriminatorType ) {
				case INTEGER -> Integer.valueOf( value );
				case CHAR -> {
					if ( value.length() != 1 ) {
						throw new IllegalArgumentException( "Expected a single character" );
					}
					yield value.charAt( 0 );
				}
				case STRING -> value;
			};
		}
		catch (IllegalArgumentException e) {
			final ModelsException modelsException = new ModelsException(
					"Invalid @Any discriminator value `" + value + "` - " + member.getName()
			);
			modelsException.addSuppressed( e );
			throw modelsException;
		}
	}

	private int defaultDiscriminatorLength(DiscriminatorType discriminatorType) {
		return discriminatorType == DiscriminatorType.STRING ? 31 : 0;
	}
}
