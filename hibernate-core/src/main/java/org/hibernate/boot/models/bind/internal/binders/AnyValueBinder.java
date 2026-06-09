/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.annotations.AnyDiscriminatorImplicitValues;
import org.hibernate.boot.models.bind.internal.sources.AnySource;
import org.hibernate.boot.models.bind.internal.sources.BasicValueSource;
import org.hibernate.boot.models.bind.internal.sources.ColumnSource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.internal.FullNameImplicitDiscriminatorStrategy;
import org.hibernate.metamodel.internal.ShortNameImplicitDiscriminatorStrategy;
import org.hibernate.metamodel.mapping.DiscriminatorValue;
import org.hibernate.metamodel.spi.ImplicitDiscriminatorStrategy;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.MemberDetails;

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
/// - `@AnyDiscriminator` chooses the discriminator Java type: `STRING`,
///   `INTEGER`, or `CHAR`.
/// - `@JdbcType` and `@JdbcTypeCode` are applied by [BasicValueBinder] to the
///   discriminator value.
/// - `@AnyDiscriminatorValue(s)` creates explicit discriminator-to-entity
///   mappings.
/// - `@AnyDiscriminatorImplicitValues` sets the mapping model's implicit
///   discriminator strategy.
/// - `@AnyKeyJavaClass` supplies the key Java type.
/// - `@AnyKeyJavaType`, `@AnyKeyJdbcType`, and `@AnyKeyJdbcTypeCode` are applied
///   by [BasicValueBinder] to the key value.
/// - Singular `@JoinColumn`, singular `@JoinTable#inverseJoinColumns`, or plural
///   `@JoinTable#inverseJoinColumns` names the key column or columns.  If absent,
///   the binder uses the current implicit key-column default for this prototype.
///
/// Not yet supported:
///
/// - cascade propagation from `@Any#cascade` / `@ManyToAny#cascade`
/// - discriminator `@Formula`
/// - inferred key Java type
/// - composite any-key type inference from target identifiers
/// - implicit singular `@Any` join-table names
/// - map-valued `@ManyToAny`
/// - optionality becoming non-optional when either explicit discriminator or key
///   column is non-nullable
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
		if ( source.implicitDiscriminatorValues() != null ) {
			any.setImplicitDiscriminatorValueStrategy( resolveImplicitDiscriminatorStrategy( source ) );
		}
		final BasicValue key = bindKey( source, propertyName, table );
		any.setKey( key );
		addAdditionalKeySelectables( any, key );
		any.setTypeUsingReflection( source.member().getDeclaringType().getName(), propertyName );
		return any;
	}

	private BasicValue bindDiscriminator(AnySource source, String propertyName, Table table) {
		final BasicValue discriminator = new BasicValue( bindingState.getMetadataBuildingContext(), table );
		discriminator.setTable( table );

		final Column column = ColumnBinder.bindColumn(
				ColumnSource.from( source.discriminatorColumn() ),
				() -> propertyName + "_type",
				false,
				source.optional(),
				defaultDiscriminatorLength( source.discriminatorType() ),
				0,
				0
		);
		table.addColumn( column );
		discriminator.addColumn( column, true, true );

		BasicValueBinder.bindBasicValue(
				BasicValueSource.anyDiscriminator( source.member(), source.discriminatorJavaType() ),
				null,
				discriminator,
				bindingOptions,
				bindingState,
				bindingContext
		);
		return discriminator;
	}

	private BasicValue bindKey(AnySource source, String propertyName, Table table) {
		final BasicValue key = new BasicValue( bindingState.getMetadataBuildingContext(), table );
		key.setTable( table );

		if ( source.keyColumns().isEmpty() ) {
			final Column column = ColumnBinder.bindColumn(
					null,
					() -> propertyName + "_id",
					false,
					source.optional()
			);
			table.addColumn( column );
			key.addColumn( column, true, true );
		}
		else {
			for ( int i = 0; i < source.keyColumns().size(); i++ ) {
				final int index = i;
				final Column column = ColumnBinder.bindColumn(
						ColumnSource.from( source.keyColumns().get( index ) ),
						() -> index == 0 ? propertyName + "_id" : propertyName + "_id" + ( index + 1 ),
						false,
						source.optional()
				);
				table.addColumn( column );
				key.addColumn( column, true, true );
			}
		}

		BasicValueBinder.bindBasicValue(
				BasicValueSource.anyKey( source.member(), source.keyJavaClass() ),
				null,
				key,
				bindingOptions,
				bindingState,
				bindingContext
		);
		return key;
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
			if ( bindingState.getTypeBinder( bindingContext.getBootstrapContext()
					.getModelsContext()
					.getClassDetailsRegistry()
					.resolveClassDetails( entityClass.getName() ) ) == null ) {
				throw new MappingException(
						"@Any discriminator value referenced an unknown entity `"
								+ entityClass.getName() + "` - " + source.member().getName()
				);
			}
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
		if ( source.keyJavaClass() == null ) {
			// todo (any) : infer the key Java type from the identifier types of the explicit or implicit targets
			throw new UnsupportedOperationException(
					"@Any requires @AnyKeyJavaClass in this binder - " + source.member().getName()
			);
		}
		if ( source.discriminatorValues().isEmpty() && source.implicitDiscriminatorValues() == null ) {
			throw new UnsupportedOperationException(
					"@Any requires explicit @AnyDiscriminatorValue mappings or @AnyDiscriminatorImplicitValues in this binder - "
							+ source.member().getName()
			);
		}
	}

	private ImplicitDiscriminatorStrategy resolveImplicitDiscriminatorStrategy(AnySource source) {
		final AnyDiscriminatorImplicitValues implicitValues = source.implicitDiscriminatorValues();
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
		return bindingContext.getBootstrapContext()
				.getCustomTypeProducer()
				.produceBeanInstance( implementation );
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
