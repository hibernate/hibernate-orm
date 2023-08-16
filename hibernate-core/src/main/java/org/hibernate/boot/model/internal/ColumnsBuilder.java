/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import static org.hibernate.boot.model.internal.AnnotatedColumn.buildColumnFromAnnotation;
import static org.hibernate.boot.model.internal.AnnotatedColumn.buildColumnFromNoAnnotation;
import static org.hibernate.boot.model.internal.AnnotatedColumn.buildColumnsFromAnnotations;
import static org.hibernate.boot.model.internal.AnnotatedColumn.buildFormulaFromAnnotation;
import static org.hibernate.boot.model.internal.BinderHelper.getOverridableAnnotation;
import static org.hibernate.boot.model.internal.BinderHelper.getPath;
import static org.hibernate.boot.model.internal.BinderHelper.getPropertyOverriddenByMapperOrMapsId;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;

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
	private final XProperty property;
	private final PropertyData inferredData;
	private final EntityBinder entityBinder;
	private final MetadataBuildingContext buildingContext;
	private AnnotatedColumns columns;
	private AnnotatedJoinColumns joinColumns;

	public ColumnsBuilder(
			PropertyHolder propertyHolder,
			Nullability nullability,
			XProperty property,
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


//		Comment comment = property.getAnnotation(Comment.class);
		if ( property.isAnnotationPresent( Column.class ) ) {
			columns = buildColumnFromAnnotation(
					property.getAnnotation( Column.class ),
//					comment,
					nullability,
					propertyHolder,
					inferredData,
					entityBinder.getSecondaryTables(),
					buildingContext
			);
		}
		else if ( property.isAnnotationPresent( Formula.class ) ) {
			columns = buildFormulaFromAnnotation(
					getOverridableAnnotation( property, Formula.class, buildingContext ),
//					comment,
					nullability,
					propertyHolder,
					inferredData,
					entityBinder.getSecondaryTables(),
					buildingContext
			);
		}
		else if ( property.isAnnotationPresent( Columns.class ) ) {
			columns = buildColumnsFromAnnotations(
					property.getAnnotation( Columns.class ).columns(),
//					comment,
					nullability,
					propertyHolder,
					inferredData,
					entityBinder.getSecondaryTables(),
					buildingContext
			);
		}

		//set default values if needed
		if ( joinColumns == null
				&& ( property.isAnnotationPresent( ManyToOne.class )
						|| property.isAnnotationPresent( OneToOne.class ) ) ) {
			joinColumns = buildDefaultJoinColumnsForToOne( property, inferredData );
		}
		else if ( joinColumns == null
				&& ( property.isAnnotationPresent( OneToMany.class )
						|| property.isAnnotationPresent( ElementCollection.class ) ) ) {
			OneToMany oneToMany = property.getAnnotation( OneToMany.class );
			joinColumns = AnnotatedJoinColumns.buildJoinColumns(
					null,
//					comment,
					oneToMany == null ? null : nullIfEmpty( oneToMany.mappedBy() ),
					entityBinder.getSecondaryTables(),
					propertyHolder,
					inferredData,
					buildingContext
			);
		}
		else if ( joinColumns == null
				&& property.isAnnotationPresent( org.hibernate.annotations.Any.class ) ) {
			throw new AnnotationException( "Property '" + getPath( propertyHolder, inferredData )
					+ "' is annotated '@Any' and must declare at least one '@JoinColumn'" );
		}
		if ( columns == null && !property.isAnnotationPresent( ManyToMany.class ) ) {
			//useful for collection of embedded elements
			columns = buildColumnFromNoAnnotation(
//					comment,
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

	private AnnotatedJoinColumns buildDefaultJoinColumnsForToOne(XProperty property, PropertyData inferredData) {
		final JoinTable joinTableAnn = propertyHolder.getJoinTable( property );
//		final Comment comment = property.getAnnotation(Comment.class);
		if ( joinTableAnn != null ) {
			return AnnotatedJoinColumns.buildJoinColumns(
					joinTableAnn.inverseJoinColumns(),
//					comment,
					null,
					entityBinder.getSecondaryTables(),
					propertyHolder,
					inferredData,
					buildingContext
			);
		}
		else {
			OneToOne oneToOneAnn = property.getAnnotation( OneToOne.class );
			return AnnotatedJoinColumns.buildJoinColumns(
					null,
//					comment,
					oneToOneAnn == null ? null : nullIfEmpty( oneToOneAnn.mappedBy() ),
					entityBinder.getSecondaryTables(),
					propertyHolder,
					inferredData,
					buildingContext
			);
		}
	}

	private AnnotatedJoinColumns buildExplicitJoinColumns(XProperty property, PropertyData inferredData) {
		// process @JoinColumns before @Columns to handle collection of entities properly
		final JoinColumn[] joinColumnAnnotations = getJoinColumnAnnotations( property, inferredData );
		if ( joinColumnAnnotations != null ) {
			return AnnotatedJoinColumns.buildJoinColumns(
					joinColumnAnnotations,
//					property.getAnnotation( Comment.class ),
					null,
					entityBinder.getSecondaryTables(),
					propertyHolder,
					inferredData,
					buildingContext
			);
		}

		final JoinColumnOrFormula[] joinColumnOrFormulaAnnotations = joinColumnOrFormulaAnnotations( property, inferredData );
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

		if ( property.isAnnotationPresent( JoinFormula.class) ) {
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

	private JoinColumnOrFormula[] joinColumnOrFormulaAnnotations(XProperty property, PropertyData inferredData) {
		if ( property.isAnnotationPresent( JoinColumnOrFormula.class ) ) {
			return new JoinColumnOrFormula[] { property.getAnnotation( JoinColumnOrFormula.class ) };
		}
		else if ( property.isAnnotationPresent( JoinColumnsOrFormulas.class ) ) {
			final JoinColumnsOrFormulas joinColumnsOrFormulas = property.getAnnotation( JoinColumnsOrFormulas.class );
			final JoinColumnOrFormula[] joinColumnOrFormula = joinColumnsOrFormulas.value();
			if ( joinColumnOrFormula.length == 0 ) {
				throw new AnnotationException( "Property '" + getPath( propertyHolder, inferredData)
						+ "' has an empty '@JoinColumnsOrFormulas' annotation" );
			}
			return joinColumnOrFormula;
		}
		else {
			return null;
		}
	}

	private JoinColumn[] getJoinColumnAnnotations(XProperty property, PropertyData inferredData) {
		if ( property.isAnnotationPresent( JoinColumn.class ) ) {
			return new JoinColumn[] { property.getAnnotation( JoinColumn.class ) };
		}
		else if ( property.isAnnotationPresent( JoinColumns.class ) ) {
			final JoinColumns joinColumns = property.getAnnotation( JoinColumns.class );
			final JoinColumn[] joinColumn = joinColumns.value();
			if ( joinColumn.length == 0 ) {
				throw new AnnotationException( "Property '" + getPath( propertyHolder, inferredData)
						+ "' has an empty '@JoinColumns' annotation" );
			}
			return joinColumn;
		}
		else {
			return null;
		}
	}

	/**
	 * Useful to override a column either by {@code @MapsId} or by {@code @IdClass}
	 */
	AnnotatedColumns overrideColumnFromMapperOrMapsIdProperty(boolean isId) {
		final PropertyData override =
				getPropertyOverriddenByMapperOrMapsId( isId, propertyHolder, property.getName(), buildingContext );
		if ( override != null ) {
			final AnnotatedJoinColumns joinColumns = buildExplicitJoinColumns( override.getProperty(), override );
			return joinColumns == null
					? buildDefaultJoinColumnsForToOne( override.getProperty(), override )
					: joinColumns;
		}
		else {
			return columns;
		}
	}

}
