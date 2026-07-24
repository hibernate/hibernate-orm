/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.sources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.mapping.internal.binders.CascadeBinder;
import org.hibernate.boot.models.annotations.internal.JoinColumnJpaAnnotation;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfileOverride;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.PropertyRef;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Fetch;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;

import static org.hibernate.boot.models.internal.DialectOverrideAnnotationHelper.getOverridableAnnotation;

/// Source-model facts for an owning to-one association value.
///
/// A to-one mapping needs both the source member and the association-specific annotation
/// family.  Keeping those together makes target type, optionality, fetch style, join table,
/// and join columns resolvable from one object instead of passing parallel annotation
/// arguments through the binder.
///
/// This follows the same design thread as [BasicValueSource], [CollectionSource], and
/// [ComponentSource]: the mapping object is richer if it retains the Hibernate Models
/// source facts that produced it.  For to-one associations, those facts are especially
/// important because the source member simultaneously describes:
///
/// - the Java type or explicit `targetEntity`
/// - the association cardinality annotation (`@ManyToOne` or `@OneToOne`)
/// - fetch and optionality
/// - join columns, possibly supplied through `@AssociationOverride`
/// - an optional join table, also possibly supplied through `@AssociationOverride`
///
/// The physical mapping currently becomes an [org.hibernate.mapping.ManyToOne] even for
/// a logical owning `@OneToOne`.  That physical representation is useful internally, but
/// it can hide the source-level role.  This record keeps the logical association source
/// visible so the binder can ask source-level questions such as [#isLogicalOneToOne()]
/// and [#optional()] without re-reading raw annotations in several places.
///
/// Association overrides are also source-level facts.  A to-one inside an embeddable may
/// receive its join columns or join table from an override declared on the owning
/// embedded attribute.  That makes "where did the join instruction come from?" separate
/// from "which mapping value receives the columns?"  A mapping-model-native source
/// descriptor could preserve that distinction directly.
///
/// In an upstream mapping-model version, a to-one mapping might directly retain:
///
/// - the source [MemberDetails]
/// - the logical association role (`many-to-one` versus owning `one-to-one`)
/// - the effective target [ClassDetails]
/// - source join-column and join-table descriptors
/// - the active association override, if any
/// - fetch and optionality source values
///
/// That would reduce the amount of binder-only annotation interpretation and make later
/// foreign-key/table-key phases more source-aware.
///
/// @since 9.0
/// @author Steve Ebersole
public record ToOneSource(
		/// The member that declared the association.
		MemberDetails member,

		/// The owner class name used when configuring reflection-based value metadata.
		///
		/// This is currently retained because [org.hibernate.mapping.ManyToOne] still
		/// expects `setTypeUsingReflection(ownerClassName, propertyName)`.  In a mapping
		/// model that directly carries source members, this may become derivable.
		String ownerClassName,

		/// The persistent property name on the owner.
		String propertyName,

		/// Attribute path passed to implicit naming strategy for implicit join columns.
		AttributePath implicitNamingPath,

		/// The direct `@ManyToOne` source annotation, if this association is many-to-one.
		jakarta.persistence.ManyToOne manyToOne,

		/// The direct `@OneToOne` source annotation, if this association is one-to-one.
		OneToOne oneToOne,

		/// The path-based association override currently active for this source, if any.
		AssociationOverride associationOverride,

		/// The models context used to resolve repeatable annotations.
		ModelsContext modelsContext,

		/// Resolved source type for generic association members.
		TypeDetails resolvedType) {
	/// Creates a source from a member and optional path-based association override.
	public static ToOneSource create(
			MemberDetails member,
			String ownerClassName,
			String propertyName,
			AssociationOverride associationOverride,
			ModelsContext modelsContext) {
		return create( member, ownerClassName, propertyName, associationOverride, modelsContext, null );
	}

	public static ToOneSource create(
			MemberDetails member,
			String ownerClassName,
			String propertyName,
			AssociationOverride associationOverride,
			ModelsContext modelsContext,
			TypeDetails resolvedType) {
		return create(
				member,
				ownerClassName,
				propertyName,
				AttributePath.parse( propertyName ),
				associationOverride,
				modelsContext,
				resolvedType
		);
	}

	public static ToOneSource create(
			MemberDetails member,
			String ownerClassName,
			String propertyName,
			AttributePath implicitNamingPath,
			AssociationOverride associationOverride,
			ModelsContext modelsContext,
			TypeDetails resolvedType) {
		return new ToOneSource(
				member,
				ownerClassName,
				propertyName,
				implicitNamingPath,
				member.getDirectAnnotationUsage( jakarta.persistence.ManyToOne.class ),
				member.getDirectAnnotationUsage( OneToOne.class ),
				associationOverride,
				modelsContext,
				resolvedType
		);
	}

	/// Whether this source represents the inverse side of a one-to-one association.
	///
	/// The current binder only supports owning to-one mappings, so this is checked early
	/// and rejected before creating the physical mapping value.
	public boolean isInverseOneToOne() {
		return oneToOne != null && StringHelper.isNotEmpty( oneToOne.mappedBy() );
	}

	/// Whether this source is logically an owning one-to-one association.
	///
	/// The physical mapping value is still [org.hibernate.mapping.ManyToOne], but this
	/// source-level flag determines whether that value is marked as logical one-to-one and
	/// whether join columns default to unique.
	public boolean isLogicalOneToOne() {
		return oneToOne != null;
	}

	/// The fetch style declared by the active association annotation.
	public FetchType fetchType() {
		return manyToOne != null ? manyToOne.fetch() : oneToOne.fetch();
	}

	/// The effective fetch style requested by graphless JPA `@Fetch`, if present,
	/// or by the active association annotation.
	public FetchType effectiveFetchType(FetchType defaultToOneFetchType) {
		if ( hibernateFetchMode() == FetchMode.JOIN ) {
			return FetchType.EAGER;
		}
		final Fetch fetch = graphlessFetch();
		if ( fetch != null && fetch.type() != FetchType.DEFAULT ) {
			return fetch.type();
		}
		return fetchType() == FetchType.LAZY ? FetchType.LAZY : defaultToOneFetchType;
	}

	private Fetch graphlessFetch() {
		if ( modelsContext == null ) {
			final Fetch fetch = member.getDirectAnnotationUsage( Fetch.class );
			return fetch != null && StringHelper.isEmpty( fetch.graph() ) && fetch.subgraph().length == 0 ? fetch : null;
		}
		final Fetch[] fetches = member.getRepeatedAnnotationUsages( Fetch.class, modelsContext );
		for ( Fetch fetch : fetches ) {
			if ( StringHelper.isEmpty( fetch.graph() ) && fetch.subgraph().length == 0 ) {
				return fetch;
			}
		}
		return null;
	}

	public FetchMode hibernateFetchMode() {
		final org.hibernate.annotations.Fetch fetch = member.getDirectAnnotationUsage( org.hibernate.annotations.Fetch.class );
		return fetch == null ? null : fetch.value();
	}

	/// The optionality declared by the active association annotation.
	public boolean optional() {
		return manyToOne != null ? manyToOne.optional() : oneToOne.optional();
	}

	/// Aggregates the JPA cascade and mapping defaults for this to-one association.
	public EnumSet<CascadeType> cascades(BindingState bindingState) {
		if ( manyToOne != null ) {
			return CascadeBinder.aggregateCascadeTypes( manyToOne.cascade(), false, bindingState );
		}
		return CascadeBinder.aggregateCascadeTypes( oneToOne.cascade(), oneToOne.orphanRemoval(), bindingState );
	}

	public boolean orphanRemoval() {
		return oneToOne != null && oneToOne.orphanRemoval();
	}

	/// Resolves the effective target entity class.
	///
	/// Explicit `targetEntity` wins over the Java member type.  Keeping this on the source
	/// object lines up with the broader idea that mapping values should retain their
	/// source-model type information instead of reducing it immediately to strings.
	public ClassDetails targetClassDetails(BindingContext bindingContext) {
		if ( manyToOne != null && manyToOne.targetEntity() != void.class ) {
			return bindingContext.getClassDetailsRegistry().resolveClassDetails( manyToOne.targetEntity().getName() );
		}
		if ( oneToOne != null && oneToOne.targetEntity() != void.class ) {
			return bindingContext.getClassDetailsRegistry().resolveClassDetails( oneToOne.targetEntity().getName() );
		}
		return resolvedType == null ? member.getType().determineRawClass() : resolvedType.determineRawClass();
	}

	/// Resolves the source join table, considering association overrides first.
	///
	/// A non-empty override join table replaces the member-level join table source.
	public JoinTable joinTable() {
		if ( associationOverride != null && isSpecified( associationOverride.joinTable() ) ) {
			return associationOverride.joinTable();
		}
		return member.getDirectAnnotationUsage( JoinTable.class );
	}

	/// Resolves the join columns that point from this association value to the target.
	///
	/// When a join table is used, the value columns come from `inverseJoinColumns`.
	/// Otherwise, they come from the normal join-column source.
	public List<JoinColumn> valueJoinColumns(JoinTable joinTable) {
		if ( joinTable != null ) {
			final List<JoinColumn> inverseJoinColumns = listJoinColumns( joinTable.inverseJoinColumns() );
			return inverseJoinColumns.isEmpty() ? joinColumns() : inverseJoinColumns;
		}
		return joinColumns();
	}

	/// Resolves value-side join columns and formulas.
	public List<JoinColumnOrFormulaSource> valueJoinColumnsOrFormulas(JoinTable joinTable, Dialect dialect) {
		if ( joinTable != null ) {
			final List<JoinColumn> inverseJoinColumns = listJoinColumns( joinTable.inverseJoinColumns() );
			return listJoinColumnSources( inverseJoinColumns.isEmpty() ? joinColumns() : inverseJoinColumns );
		}

		final JoinColumnsOrFormulas joinColumnsOrFormulasAnn = member.getDirectAnnotationUsage( JoinColumnsOrFormulas.class );
		if ( joinColumnsOrFormulasAnn != null ) {
			return listJoinColumnOrFormulaSources( joinColumnsOrFormulasAnn.value() );
		}

		final JoinColumnOrFormula[] joinColumnOrFormulas =
				member.getRepeatedAnnotationUsages( JoinColumnOrFormula.class, modelsContext );
		if ( joinColumnOrFormulas.length > 0 ) {
			return listJoinColumnOrFormulaSources( joinColumnOrFormulas );
		}

		final JoinFormula joinFormula = getOverridableAnnotation(
				member,
				JoinFormula.class,
				dialect,
				modelsContext
		);
		if ( joinFormula != null ) {
			return List.of( JoinColumnOrFormulaSource.formula( joinFormula ) );
		}

		return listJoinColumnSources( joinColumns() );
	}

	/// Resolves foreign-key metadata for the value-side columns.
	public ForeignKeySource valueForeignKeySource(JoinTable joinTable) {
		if ( notFound() != null ) {
			return ForeignKeySource.noConstraint();
		}
		if ( joinTable != null ) {
			final List<JoinColumn> inverseJoinColumns = listJoinColumns( joinTable.inverseJoinColumns() );
			final List<JoinColumn> valueJoinColumns = inverseJoinColumns.isEmpty() ? joinColumns() : inverseJoinColumns;
			return ForeignKeySource.firstSpecified(
					ForeignKeySource.fromFirstSpecifiedJoinColumn( valueJoinColumns ),
					ForeignKeySource.inverseFrom( joinTable )
			);
		}
		if ( associationOverride != null ) {
			return ForeignKeySource.firstSpecified(
					ForeignKeySource.fromFirstSpecifiedJoinColumn( listJoinColumns( associationOverride.joinColumns() ) ),
					ForeignKeySource.from( associationOverride )
			);
		}
		final JoinColumns joinColumnsAnn = member.getDirectAnnotationUsage( JoinColumns.class );
		if ( joinColumnsAnn != null ) {
			return ForeignKeySource.firstSpecified(
					ForeignKeySource.fromFirstSpecifiedJoinColumn( listJoinColumns( joinColumnsAnn.value() ) ),
					ForeignKeySource.from( joinColumnsAnn )
			);
		}
		final List<JoinColumn> joinColumns = joinColumns();
		return ForeignKeySource.fromFirstSpecifiedJoinColumn( joinColumns );
	}

	/// Resolves the normal source join columns, considering association overrides first.
	///
	/// This method intentionally returns source annotations, not physical columns.  The
	/// physical columns are still created later after target identifier column ordering and
	/// defaults are known.
	public List<JoinColumn> joinColumns() {
		if ( associationOverride != null && associationOverride.joinColumns().length > 0 ) {
			return listJoinColumns( associationOverride.joinColumns() );
		}

		final JoinColumns joinColumnsAnn = member.getDirectAnnotationUsage( JoinColumns.class );
		if ( joinColumnsAnn != null ) {
			return listJoinColumns( joinColumnsAnn.value() );
		}

		final JoinColumn joinColumnAnn = member.getDirectAnnotationUsage( JoinColumn.class );
		if ( joinColumnAnn != null ) {
			return List.of( joinColumnAnn );
		}

		final PrimaryKeyJoinColumns primaryKeyJoinColumnsAnn = member.getDirectAnnotationUsage( PrimaryKeyJoinColumns.class );
		if ( primaryKeyJoinColumnsAnn != null ) {
			return listPrimaryKeyJoinColumns( primaryKeyJoinColumnsAnn.value() );
		}

		final PrimaryKeyJoinColumn primaryKeyJoinColumnAnn = member.getDirectAnnotationUsage( PrimaryKeyJoinColumn.class );
		return primaryKeyJoinColumnAnn == null
				? List.of()
			: List.of( JoinColumnJpaAnnotation.toJoinColumn( primaryKeyJoinColumnAnn, modelsContext ) );
	}

	public boolean hasExplicitJoinColumnSpecification() {
		return associationOverride != null && associationOverride.joinColumns().length > 0
				|| member.getDirectAnnotationUsage( JoinColumns.class ) != null
				|| member.getDirectAnnotationUsage( JoinColumn.class ) != null
				|| member.getDirectAnnotationUsage( PrimaryKeyJoinColumns.class ) != null
				|| member.getDirectAnnotationUsage( PrimaryKeyJoinColumn.class ) != null;
	}

	public boolean hasExplicitJoinColumnOverride() {
		return associationOverride != null && associationOverride.joinColumns().length > 0
				|| member.getDirectAnnotationUsage( JoinColumns.class ) != null
				|| member.getDirectAnnotationUsage( JoinColumn.class ) != null;
	}

	public boolean hasPrimaryKeyJoinColumnSpecification() {
		return member.getDirectAnnotationUsage( PrimaryKeyJoinColumns.class ) != null
				|| member.getDirectAnnotationUsage( PrimaryKeyJoinColumn.class ) != null;
	}

	public NotFound notFound() {
		return member.getDirectAnnotationUsage( NotFound.class );
	}

	public PropertyRef propertyRef() {
		return member.getDirectAnnotationUsage( PropertyRef.class );
	}

	public FetchProfileOverride[] fetchProfileOverrides() {
		return member.getRepeatedAnnotationUsages( FetchProfileOverride.class, modelsContext );
	}

	/// Whether a join table annotation carries any meaningful source information.
	private static boolean isSpecified(JoinTable joinTable) {
		return joinTable != null
				&& ( StringHelper.isNotEmpty( joinTable.name() )
						|| joinTable.joinColumns().length > 0
						|| joinTable.inverseJoinColumns().length > 0 );
	}

	/// Converts annotation arrays to lists at the source boundary.
	private static List<JoinColumn> listJoinColumns(JoinColumn[] joinColumns) {
		if ( joinColumns.length == 0 ) {
			return List.of();
		}
		final ArrayList<JoinColumn> result = new ArrayList<>( joinColumns.length );
		result.addAll( Arrays.asList( joinColumns ) );
		return result;
	}

	private static List<JoinColumnOrFormulaSource> listJoinColumnSources(List<JoinColumn> joinColumns) {
		if ( joinColumns.isEmpty() ) {
			return List.of();
		}
		final ArrayList<JoinColumnOrFormulaSource> result = new ArrayList<>( joinColumns.size() );
		for ( JoinColumn joinColumn : joinColumns ) {
			result.add( JoinColumnOrFormulaSource.column( joinColumn ) );
		}
		return result;
	}

	private static List<JoinColumnOrFormulaSource> listJoinColumnSources(JoinColumn[] joinColumns) {
		return listJoinColumnSources( listJoinColumns( joinColumns ) );
	}

	private static List<JoinColumnOrFormulaSource> listJoinColumnOrFormulaSources(
			JoinColumnOrFormula[] joinColumnsOrFormulas) {
		final ArrayList<JoinColumnOrFormulaSource> result = new ArrayList<>( joinColumnsOrFormulas.length );
		for ( JoinColumnOrFormula joinColumnOrFormula : joinColumnsOrFormulas ) {
			if ( StringHelper.isNotEmpty( joinColumnOrFormula.formula().value() ) ) {
				result.add( JoinColumnOrFormulaSource.formula( joinColumnOrFormula.formula() ) );
			}
			else {
				result.add( JoinColumnOrFormulaSource.column( joinColumnOrFormula.column() ) );
			}
		}
		return result;
	}

	private List<JoinColumn> listPrimaryKeyJoinColumns(PrimaryKeyJoinColumn[] joinColumns) {
		if ( joinColumns.length == 0 ) {
			return List.of();
		}
		final ArrayList<JoinColumn> result = new ArrayList<>( joinColumns.length );
		for ( PrimaryKeyJoinColumn joinColumn : joinColumns ) {
			result.add( JoinColumnJpaAnnotation.toJoinColumn( joinColumn, modelsContext ) );
		}
		return result;
	}

	public record JoinColumnOrFormulaSource(JoinColumn column, JoinFormula formula) {
		public static JoinColumnOrFormulaSource column(JoinColumn column) {
			return new JoinColumnOrFormulaSource( column, null );
		}

		public static JoinColumnOrFormulaSource formula(JoinFormula formula) {
			return new JoinColumnOrFormulaSource( null, formula );
		}

		public String referencedColumnName() {
			return formula == null ? column.referencedColumnName() : formula.referencedColumnName();
		}
	}
}
