/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Any;
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

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

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

		if ( property.hasDirectAnnotationUsage( Column.class ) ) {
			columns = buildColumnFromAnnotation(
					property.getDirectAnnotationUsage( Column.class ),
					property.getDirectAnnotationUsage( FractionalSeconds.class ),
					nullability,
					propertyHolder,
					inferredData,
					entityBinder.getSecondaryTables(),
					buildingContext
			);
		}
		else if ( property.hasDirectAnnotationUsage( Formula.class) ) {
			columns = buildFormulaFromAnnotation(
					getOverridableAnnotation( property, Formula.class, buildingContext ),
					nullability,
					propertyHolder,
					inferredData,
					entityBinder.getSecondaryTables(),
					buildingContext
			);
		}
		else if ( property.hasDirectAnnotationUsage( Columns.class ) ) {
			columns = buildColumnsFromAnnotations(
					property.getDirectAnnotationUsage( Columns.class ).columns(),
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
			final var oneToMany = property.getDirectAnnotationUsage( OneToMany.class );
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
				&& property.hasDirectAnnotationUsage( Any.class ) ) {
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
		final var joinTable = propertyHolder.getJoinTable( property );
		if ( joinTable != null ) {
			return AnnotatedJoinColumns.buildJoinColumns(
					joinTable.inverseJoinColumns(),
					null,
					entityBinder.getSecondaryTables(),
					propertyHolder,
					inferredData,
					buildingContext
			);
		}
		else {
			final var oneToOne = property.getDirectAnnotationUsage( OneToOne.class );
			return AnnotatedJoinColumns.buildJoinColumns(
					null,
					oneToOne == null ? null : nullIfEmpty( oneToOne.mappedBy() ),
					entityBinder.getSecondaryTables(),
					propertyHolder,
					inferredData,
					buildingContext
			);
		}
	}

	private AnnotatedJoinColumns buildExplicitJoinColumns(MemberDetails property, PropertyData inferredData) {
		// process @JoinColumns before @Columns to handle collection of entities properly
		final var joinColumns = getJoinColumnAnnotations( property );
		if ( joinColumns != null ) {
			return AnnotatedJoinColumns.buildJoinColumns(
					joinColumns,
					null,
					entityBinder.getSecondaryTables(),
					propertyHolder,
					inferredData,
					buildingContext
			);
		}

		final var joinColumnOrFormulas = joinColumnOrFormulaAnnotations( property );
		if ( joinColumnOrFormulas != null ) {
			return AnnotatedJoinColumns.buildJoinColumnsOrFormulas(
					joinColumnOrFormulas,
					null,
					entityBinder.getSecondaryTables(),
					propertyHolder,
					inferredData,
					buildingContext
			);
		}

		if ( property.hasDirectAnnotationUsage( JoinFormula.class ) ) {
			return AnnotatedJoinColumns.buildJoinColumnsWithFormula(
					getOverridableAnnotation( property, JoinFormula.class, buildingContext ),
					entityBinder.getSecondaryTables(),
					propertyHolder,
					inferredData,
					buildingContext
			);
		}

		return null;
	}

	private JoinColumnOrFormula[] joinColumnOrFormulaAnnotations(MemberDetails property) {
		final var annotations = property.getRepeatedAnnotationUsages(
				HibernateAnnotations.JOIN_COLUMN_OR_FORMULA,
				buildingContext.getBootstrapContext().getModelsContext()
		);
		return isNotEmpty( annotations ) ? annotations : null;
	}

	private JoinColumn[] getJoinColumnAnnotations(MemberDetails property) {
		final var modelsContext = buildingContext.getBootstrapContext().getModelsContext();
		final var joinColumns = property.getRepeatedAnnotationUsages( JpaAnnotations.JOIN_COLUMN, modelsContext );
		if ( isNotEmpty( joinColumns ) ) {
			return joinColumns;
		}
		else if ( property.hasDirectAnnotationUsage( MapsId.class ) ) {
			// inelegant solution to HHH-16463, let the PrimaryKeyJoinColumn
			// masquerade as a regular JoinColumn (when a @OneToOne maps to
			// the primary key of the child table, it's more elegant and more
			// spec-compliant to map the association with @PrimaryKeyJoinColumn)
			final var primaryKeyJoinColumns =
					property.getRepeatedAnnotationUsages( JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN, modelsContext );
			if ( isNotEmpty( primaryKeyJoinColumns ) ) {
				final var adapters = new JoinColumn[primaryKeyJoinColumns.length];
				for ( int i = 0; i < primaryKeyJoinColumns.length; i++ ) {
					adapters[i] = JoinColumnJpaAnnotation.toJoinColumn( primaryKeyJoinColumns[i], modelsContext );
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
			final var memberDetails = override.getAttributeMember();
			final var joinColumns = buildExplicitJoinColumns( memberDetails, override );
			return joinColumns == null
					? buildDefaultJoinColumnsForToOne( memberDetails, override )
					: joinColumns;
		}
		else {
			return columns;
		}
	}

}
