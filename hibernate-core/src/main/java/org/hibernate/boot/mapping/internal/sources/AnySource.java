/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.sources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorImplicitValues;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.boot.mapping.internal.binders.CascadeBinder;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;

import static org.hibernate.boot.models.internal.DialectOverrideAnnotationHelper.getOverridableAnnotation;

/// Source-model facts for a Hibernate `@Any` association value.
///
/// An any-valued association is represented by two basic values:
///
/// - a discriminator value that identifies the target entity type
/// - a key value that identifies the target entity instance
///
/// Those two values are physically columns/formulas on the owning table, but they
/// are semantically one association.  This source descriptor keeps the member,
/// discriminator metadata, key metadata, and discriminator mappings together so a
/// shared value binder can later be reused by singular `@Any` and plural
/// `@ManyToAny` collection elements.
///
/// The annotation ownership is intentionally split:
///
/// - `@Any` / `@ManyToAny` controls association options such as fetch, cascade,
///   and singular optionality.
/// - `@Column` or `@Formula` controls the discriminator storage.
/// - `@AnyDiscriminator`, `@JdbcType`, and `@JdbcTypeCode` describe the
///   discriminator type.  `@AnyDiscriminator` controls the Java-side
///   discriminator type, while the JDBC annotations are applied to the
///   discriminator value by the basic-value binder.
/// - `@AnyDiscriminatorValue(s)` and `@AnyDiscriminatorImplicitValues` describe
///   discriminator-to-entity resolution.
/// - `@AnyKeyJavaClass`, `@AnyKeyJavaType`, `@AnyKeyJdbcType`, and
///   `@AnyKeyJdbcTypeCode` describe the key/id value type.
/// - For singular `@Any`, `@JoinColumn` controls the key/id column.
/// - For singular `@Any`, `@JoinTable` controls an association table when
///   present, `@JoinTable#joinColumns` controls the owner key, and
///   `@JoinTable#inverseJoinColumns` controls the ANY key/id column.
/// - For plural `@ManyToAny`, `@JoinTable` controls the collection table,
///   `@JoinTable#joinColumns` controls the owner key, and
///   `@JoinTable#inverseJoinColumns` controls the ANY key/id column.
///
/// The current binder supports a conservative subset of that model:
///
/// - singular `@Any`
/// - plural `@ManyToAny`
/// - explicit or implicit `@ManyToAny` collection tables
/// - explicit singular `@Any` association tables
/// - singular and plural composite any key columns
/// - explicit `@AnyKeyJavaClass`, or inference from explicit discriminator
///   target identifiers
/// - explicit discriminator values, an explicit implicit discriminator strategy,
///   or the default full-name implicit strategy
/// - discriminator storage through `@Column` or `@Formula`
/// - discriminator Java type selection through `@AnyDiscriminator`
/// - key Java/JDBC type overrides through the `@AnyKey...` annotations
/// - cascade aggregation from `@Any#cascade`, `@ManyToAny#cascade`, and mapping defaults
///
/// @since 9.0
/// @author Steve Ebersole
public record AnySource(
		MemberDetails member,
		boolean lazy,
		boolean optional,
		EnumSet<CascadeType> cascades,
		Column discriminatorColumn,
		Formula discriminatorFormula,
		AnyDiscriminator discriminator,
		List<AnyDiscriminatorValue> discriminatorValues,
		AnyDiscriminatorImplicitValues implicitDiscriminatorValues,
		JoinTable joinTable,
		List<JoinColumn> keyColumns,
		Class<?> keyJavaClass) {

	public static AnySource create(MemberDetails member, BindingContext bindingContext, BindingState bindingState) {
		final Any any = member.getDirectAnnotationUsage( Any.class );
		if ( any == null ) {
			throw new ModelsException( "Missing @Any annotation - " + member.getName() );
		}

		final var modelsContext = bindingContext.getModelsContext();
		final var anyKeyJavaClass = member.locateAnnotationUsage( AnyKeyJavaClass.class, modelsContext );
		final JoinTable joinTable = member.getDirectAnnotationUsage( JoinTable.class );
		final List<JoinColumn> keyColumns = joinTable == null
				? joinColumns( member )
				: listJoinColumns( joinTable.inverseJoinColumns() );
			return new AnySource(
					member,
					any.fetch() == FetchType.LAZY,
					any.optional(),
					CascadeBinder.aggregateCascadeTypes( any.cascade(), false, bindingState ),
					member.getDirectAnnotationUsage( Column.class ),
					effectiveFormula( member, bindingState ),
					member.locateAnnotationUsage( AnyDiscriminator.class, modelsContext ),
					discriminatorValues( member, bindingContext ),
					member.locateAnnotationUsage( AnyDiscriminatorImplicitValues.class, modelsContext ),
					joinTable,
					keyColumns,
					anyKeyJavaClass == null ? null : anyKeyJavaClass.value()
			);
	}

	public static AnySource createManyToAny(
			CollectionSource collectionSource,
			BindingContext bindingContext,
			BindingState bindingState) {
		final MemberDetails member = collectionSource.member();
		final ManyToAny manyToAny = member.getDirectAnnotationUsage( ManyToAny.class );
		if ( manyToAny == null ) {
			throw new ModelsException( "Missing @ManyToAny annotation - " + member.getName() );
		}

		final List<JoinColumn> inverseJoinColumns = collectionSource.associationInverseJoinColumns();

		final var modelsContext = bindingContext.getModelsContext();
		final var anyKeyJavaClass = member.locateAnnotationUsage( AnyKeyJavaClass.class, modelsContext );
		return new AnySource(
				member,
				manyToAny.fetch() == FetchType.LAZY,
				true,
				CascadeBinder.aggregateCascadeTypes( manyToAny.cascade(), false, bindingState ),
				member.getDirectAnnotationUsage( Column.class ),
				effectiveFormula( member, bindingState ),
				member.locateAnnotationUsage( AnyDiscriminator.class, modelsContext ),
				discriminatorValues( member, bindingContext ),
				member.locateAnnotationUsage( AnyDiscriminatorImplicitValues.class, modelsContext ),
				collectionSource.joinTable(),
				inverseJoinColumns,
				anyKeyJavaClass == null ? null : anyKeyJavaClass.value()
		);
	}

	private static Formula effectiveFormula(MemberDetails member, BindingState bindingState) {
		return getOverridableAnnotation(
				member,
				Formula.class,
				bindingState.getDatabase().getDialect(),
				bindingState.getMetadataBuildingContext().getModelsContext()
		);
	}

	public DiscriminatorType discriminatorType() {
		return discriminator == null ? DiscriminatorType.STRING : discriminator.value();
	}

	public Class<?> discriminatorJavaType() {
		return switch ( discriminatorType() ) {
			case INTEGER -> Integer.class;
			case CHAR -> Character.class;
			case STRING -> String.class;
		};
	}

	public boolean effectiveOptional() {
		if ( !optional ) {
			return false;
		}
		if ( discriminatorColumn != null && !discriminatorColumn.nullable() ) {
			return false;
		}
		for ( JoinColumn keyColumn : keyColumns ) {
			if ( !keyColumn.nullable() ) {
				return false;
			}
		}
		return true;
	}

	private static List<AnyDiscriminatorValue> discriminatorValues(
			MemberDetails member,
			BindingContext bindingContext) {
		final ArrayList<AnyDiscriminatorValue> result = new ArrayList<>();
		if ( bindingContext != null ) {
			result.addAll( Arrays.asList( member.getRepeatedAnnotationUsages(
					AnyDiscriminatorValue.class,
					bindingContext.getModelsContext()
			) ) );
		}
		if ( result.isEmpty() ) {
			final AnyDiscriminatorValues values = bindingContext == null
					? member.getDirectAnnotationUsage( AnyDiscriminatorValues.class )
					: member.locateAnnotationUsage(
							AnyDiscriminatorValues.class,
							bindingContext.getModelsContext()
					);
			if ( values != null ) {
				result.addAll( Arrays.asList( values.value() ) );
			}
			else {
				final AnyDiscriminatorValue value = bindingContext == null
						? member.getDirectAnnotationUsage( AnyDiscriminatorValue.class )
						: member.locateAnnotationUsage(
								AnyDiscriminatorValue.class,
								bindingContext.getModelsContext()
						);
				if ( value != null ) {
					result.add( value );
				}
			}
		}
		return List.copyOf( result );
	}

	public List<JoinColumn> ownerJoinColumns() {
		return joinTable == null ? List.of() : listJoinColumns( joinTable.joinColumns() );
	}

	private static List<JoinColumn> joinColumns(MemberDetails member) {
		final JoinColumns joinColumnsAnn = member.getDirectAnnotationUsage( JoinColumns.class );
		if ( joinColumnsAnn != null ) {
			return listJoinColumns( joinColumnsAnn.value() );
		}

		final JoinColumn joinColumnAnn = member.getDirectAnnotationUsage( JoinColumn.class );
		return joinColumnAnn == null ? List.of() : List.of( joinColumnAnn );
	}

	private static List<JoinColumn> listJoinColumns(JoinColumn[] joinColumns) {
		if ( joinColumns.length == 0 ) {
			return List.of();
		}
		final ArrayList<JoinColumn> result = new ArrayList<>( joinColumns.length );
		result.addAll( Arrays.asList( joinColumns ) );
		return List.copyOf( result );
	}
}
