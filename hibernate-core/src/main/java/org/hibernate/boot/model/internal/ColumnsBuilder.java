/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import jakarta.persistence.ForeignKey;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;
import org.hibernate.AnnotationException;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.FractionalSeconds;
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

import java.lang.annotation.Annotation;

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
					property.getAnnotation( FractionalSeconds.class ),
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
					null,
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
					property.getAnnotation( FractionalSeconds.class ),
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
			final OneToOne oneToOneAnn = property.getAnnotation( OneToOne.class );
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
		else if ( property.isAnnotationPresent( MapsId.class ) ) {
			// inelegant solution to HHH-16463, let the PrimaryKeyJoinColumn
			// masquerade as a regular JoinColumn (when a @OneToOne maps to
			// the primary key of the child table, it's more elegant and more
			// spec-compliant to map the association with @PrimaryKeyJoinColumn)
			if ( property.isAnnotationPresent( PrimaryKeyJoinColumn.class ) ) {
				final PrimaryKeyJoinColumn column = property.getAnnotation( PrimaryKeyJoinColumn.class );
				return new JoinColumn[] { new JoinColumnAdapter( column ) };
			}
			else if ( property.isAnnotationPresent( PrimaryKeyJoinColumns.class ) ) {
				final PrimaryKeyJoinColumns primaryKeyJoinColumns = property.getAnnotation( PrimaryKeyJoinColumns.class );
				final JoinColumn[] joinColumns = new JoinColumn[primaryKeyJoinColumns.value().length];
				final PrimaryKeyJoinColumn[] columns = primaryKeyJoinColumns.value();
				if ( columns.length == 0 ) {
					throw new AnnotationException( "Property '" + getPath( propertyHolder, inferredData)
							+ "' has an empty '@PrimaryKeyJoinColumns' annotation" );
				}
				for ( int i = 0; i < columns.length; i++ ) {
					joinColumns[i] = new JoinColumnAdapter( columns[i] );
				}
				return joinColumns;
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

	@SuppressWarnings("ClassExplicitlyAnnotation")
	private static final class JoinColumnAdapter implements JoinColumn {
		private final PrimaryKeyJoinColumn column;

		public JoinColumnAdapter(PrimaryKeyJoinColumn column) {
			this.column = column;
		}

		@Override
		public String name() {
			return column.name();
		}

		@Override
		public String referencedColumnName() {
			return column.referencedColumnName();
		}

		@Override
		public boolean unique() {
			return false;
		}

		@Override
		public boolean nullable() {
			return false;
		}

		@Override
		public boolean insertable() {
			return false;
		}

		@Override
		public boolean updatable() {
			return false;
		}

		@Override
		public String columnDefinition() {
			return column.columnDefinition();
		}

		@Override
		public String table() {
			return "";
		}

		@Override
		public ForeignKey foreignKey() {
			return column.foreignKey();
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return JoinColumn.class;
		}
	}
}
