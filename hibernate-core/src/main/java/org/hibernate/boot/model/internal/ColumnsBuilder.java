/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.FractionalSeconds;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.JoinColumnJpaAnnotation;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;

import static org.hibernate.boot.model.internal.AnnotatedColumn.buildColumnFromAnnotation;
import static org.hibernate.boot.model.internal.AnnotatedColumn.buildColumnFromNoAnnotation;
import static org.hibernate.boot.model.internal.AnnotatedColumn.buildColumnsFromAnnotations;
import static org.hibernate.boot.model.internal.AnnotatedColumn.buildFormulaFromAnnotation;
import static org.hibernate.boot.model.internal.BinderHelper.getPath;
import static org.hibernate.boot.model.internal.DialectOverridesAnnotationHelper.getOverridableAnnotation;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

/**
 * Do the initial discovery of columns metadata and apply defaults.
 * Also hosts some convenient methods related to column processing
 *
 * @author Emmanuel Bernard
 * @author Brett Meyer
 */
class ColumnsBuilder {

	private final PropertyHolder propertyHolder;
	private final Nullability nullability;
	private final MemberDetails property;
	private final PropertyData inferredData;
	private final EntityBinder entityBinder;
	private final MetadataBuildingContext buildingContext;
	private AnnotatedColumns columns;
	private AnnotatedJoinColumns joinColumns;

	public ColumnsBuilder(
			PropertyHolder propertyHolder,
			Nullability nullability,
			MemberDetails property,
			PropertyData inferredData,
			EntityBinder entityBinder,
			MetadataBuildingContext buildingContext) {
		this.propertyHolder = propertyHolder;
		this.nullability = nullability;
		this.property = property;
		this.inferredData = inferredData;
		this.entityBinder = entityBinder;
		this.buildingContext = buildingContext;
	}

	public AnnotatedColumns getColumns() {
		return columns;
	}

	public AnnotatedJoinColumns getJoinColumns() {
		return joinColumns;
	}

	public ColumnsBuilder extractMetadata() {
		columns = null;
		joinColumns = buildExplicitJoinColumns( property, inferredData );

		final Column columnAnn = property.getDirectAnnotationUsage( Column.class );
		final Columns columnsAnn = property.getDirectAnnotationUsage( Columns.class );
		final Formula formulaAnn = property.getDirectAnnotationUsage( Formula.class );

		if ( columnAnn != null ) {
			columns = buildColumnFromAnnotation(
					columnAnn,
					property.getDirectAnnotationUsage( FractionalSeconds.class ),
					nullability,
					propertyHolder,
					inferredData,
					entityBinder.getSecondaryTables(),
					buildingContext
			);
		}
		else if ( formulaAnn != null ) {
			columns = buildFormulaFromAnnotation(
					getOverridableAnnotation( property, Formula.class, buildingContext ),
					nullability,
					propertyHolder,
					inferredData,
					entityBinder.getSecondaryTables(),
					buildingContext
			);
		}
		else if ( columnsAnn != null ) {
			columns = buildColumnsFromAnnotations(
					columnsAnn.columns(),
					null,
					nullability,
					propertyHolder,
					inferredData,
					entityBinder.getSecondaryTables(),
					buildingContext
			);
		}

		//set default values if needed
		if ( joinColumns == null
				&& ( property.hasDirectAnnotationUsage( ManyToOne.class )
						|| property.hasDirectAnnotationUsage( OneToOne.class ) ) ) {
			joinColumns = buildDefaultJoinColumnsForToOne( property, inferredData );
		}
		else if ( joinColumns == null
				&& ( property.hasDirectAnnotationUsage( OneToMany.class )
						|| property.hasDirectAnnotationUsage( ElementCollection.class ) ) ) {
			OneToMany oneToMany = property.getDirectAnnotationUsage( OneToMany.class );
			joinColumns = AnnotatedJoinColumns.buildJoinColumns(
					null,
					oneToMany == null ? null : nullIfEmpty( oneToMany.mappedBy() ),
					entityBinder.getSecondaryTables(),
					propertyHolder,
					inferredData,
					buildingContext
			);
		}
		else if ( joinColumns == null
				&& property.hasDirectAnnotationUsage( org.hibernate.annotations.Any.class ) ) {
			throw new AnnotationException( "Property '" + getPath( propertyHolder, inferredData )
					+ "' is annotated '@Any' and must declare at least one '@JoinColumn'" );
		}
		if ( columns == null && !property.hasDirectAnnotationUsage( ManyToMany.class ) ) {
			//useful for collection of embedded elements
			columns = buildColumnFromNoAnnotation(
					property.getDirectAnnotationUsage( FractionalSeconds.class ),
					nullability,
					propertyHolder,
					inferredData,
					entityBinder.getSecondaryTables(),
					buildingContext
			);
		}

		if ( nullability == Nullability.FORCED_NOT_NULL ) {
			//force columns to not null
			for ( AnnotatedColumn column : columns.getColumns() ) {
				column.forceNotNull();
			}
		}
		return this;
	}

	private AnnotatedJoinColumns buildDefaultJoinColumnsForToOne(
			MemberDetails property,
			PropertyData inferredData) {
		final JoinTable joinTableAnn = propertyHolder.getJoinTable( property );
		if ( joinTableAnn != null ) {
			return AnnotatedJoinColumns.buildJoinColumns(
					joinTableAnn.inverseJoinColumns(),
					null,
					entityBinder.getSecondaryTables(),
					propertyHolder,
					inferredData,
					buildingContext
			);
		}
		else {
			final OneToOne oneToOneAnn = property.getDirectAnnotationUsage( OneToOne.class );
			return AnnotatedJoinColumns.buildJoinColumns(
					null,
					oneToOneAnn == null ? null : nullIfEmpty( oneToOneAnn.mappedBy() ),
					entityBinder.getSecondaryTables(),
					propertyHolder,
					inferredData,
					buildingContext
			);
		}
	}

	private AnnotatedJoinColumns buildExplicitJoinColumns(MemberDetails property, PropertyData inferredData) {
		// process @JoinColumns before @Columns to handle collection of entities properly
		final JoinColumn[] joinColumnAnnotations = getJoinColumnAnnotations( property );
		if ( joinColumnAnnotations != null ) {
			return AnnotatedJoinColumns.buildJoinColumns(
					joinColumnAnnotations,
					null,
					entityBinder.getSecondaryTables(),
					propertyHolder,
					inferredData,
					buildingContext
			);
		}

		final JoinColumnOrFormula[] joinColumnOrFormulaAnnotations = joinColumnOrFormulaAnnotations( property );
		if ( joinColumnOrFormulaAnnotations != null ) {
			return AnnotatedJoinColumns.buildJoinColumnsOrFormulas(
					joinColumnOrFormulaAnnotations,
					null,
					entityBinder.getSecondaryTables(),
					propertyHolder,
					inferredData,
					buildingContext
			);
		}

		if ( property.hasDirectAnnotationUsage( JoinFormula.class) ) {
			final JoinFormula joinFormula = getOverridableAnnotation( property, JoinFormula.class, buildingContext );
			return AnnotatedJoinColumns.buildJoinColumnsWithFormula(
					joinFormula,
					entityBinder.getSecondaryTables(),
					propertyHolder,
					inferredData,
					buildingContext
			);
		}

		return null;
	}

	private JoinColumnOrFormula[] joinColumnOrFormulaAnnotations(MemberDetails property) {
		final ModelsContext modelsContext = buildingContext.getBootstrapContext().getModelsContext();
		final JoinColumnOrFormula[] annotations = property.getRepeatedAnnotationUsages(
				HibernateAnnotations.JOIN_COLUMN_OR_FORMULA,
				modelsContext
		);
		return isNotEmpty( annotations ) ? annotations : null;
	}

	private JoinColumn[] getJoinColumnAnnotations(MemberDetails property) {
		final ModelsContext modelsContext = buildingContext.getBootstrapContext().getModelsContext();

		final JoinColumn[] joinColumns = property.getRepeatedAnnotationUsages(
				JpaAnnotations.JOIN_COLUMN,
				modelsContext
		);
		if ( isNotEmpty( joinColumns ) ) {
			return joinColumns;
		}
		else if ( property.hasDirectAnnotationUsage( MapsId.class ) ) {
			// inelegant solution to HHH-16463, let the PrimaryKeyJoinColumn
			// masquerade as a regular JoinColumn (when a @OneToOne maps to
			// the primary key of the child table, it's more elegant and more
			// spec-compliant to map the association with @PrimaryKeyJoinColumn)
			final PrimaryKeyJoinColumn[] primaryKeyJoinColumns = property.getRepeatedAnnotationUsages(
					JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN,
					modelsContext
			);
			if ( isNotEmpty( primaryKeyJoinColumns ) ) {
				final JoinColumn[] adapters = new JoinColumn[primaryKeyJoinColumns.length];
				for ( int i = 0; i < primaryKeyJoinColumns.length; i++ ) {
					final PrimaryKeyJoinColumn primaryKeyJoinColumn = primaryKeyJoinColumns[i];
					adapters[i] = JoinColumnJpaAnnotation.toJoinColumn( primaryKeyJoinColumn, modelsContext );
				}
				return adapters;
			}
			else {
				return null;
			}
		}
		else {
			return null;
		}
	}

	/**
	 * Useful to override a column either by {@code @MapsId} or by {@code @IdClass}
	 */
	AnnotatedColumns overrideColumnFromMapperOrMapsIdProperty(PropertyData override) {
		if ( override != null ) {
			final MemberDetails attributeMember = override.getAttributeMember();
			final AnnotatedJoinColumns joinColumns = buildExplicitJoinColumns( attributeMember, override );
			return joinColumns == null
					? buildDefaultJoinColumnsForToOne( attributeMember, override )
					: joinColumns;
		}
		else {
			return columns;
		}
	}

}
