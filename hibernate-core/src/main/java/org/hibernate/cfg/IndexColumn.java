/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;
import java.util.Map;
import jakarta.persistence.OrderColumn;

import org.hibernate.annotations.ListIndexBase;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Join;

import static org.hibernate.cfg.BinderHelper.isEmptyAnnotationValue;

/**
 * An {@link jakarta.persistence.OrderColumn} annotation
 *
 * @author inger
 */
public class IndexColumn extends AnnotatedColumn {
	private int base;

	public IndexColumn() {
		setLength( 0L );
		setPrecision( 0 );
		setScale( 0 );
	}

	public static IndexColumn fromAnnotations(
			OrderColumn jpaAnnotation,
			org.hibernate.annotations.IndexColumn hibAnnotation,
			ListIndexBase indexBaseAnnotation,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {
		final IndexColumn column;
		if ( jpaAnnotation != null ) {
			column = buildColumnFromAnnotation(
					jpaAnnotation,
					propertyHolder,
					inferredData,
					secondaryTables,
					context
			);
		}
		else if ( hibAnnotation != null ) {
			column = buildColumnFromAnnotation(
					hibAnnotation,
					propertyHolder,
					inferredData,
					context
			);
			column.setBase( hibAnnotation.base() );
		}
		else {
			column = new IndexColumn();
			column.setLogicalColumnName( inferredData.getPropertyName() + "_ORDER" ); //JPA default name
			column.setImplicit( true );
			column.setBuildingContext( context );
			column.setPropertyHolder( propertyHolder );
			column.bind();
		}

		if ( indexBaseAnnotation != null ) {
			column.setBase( indexBaseAnnotation.value() );
		}

		return column;
	}

	public int getBase() {
		return base;
	}

	public void setBase(int base) {
		this.base = base;
	}

	/**
	 * JPA 2 {@link OrderColumn @OrderColumn} processing.
	 *
	 * @param ann The OrderColumn annotation instance
	 * @param propertyHolder Information about the property
	 * @param inferredData Yeah, right.  Uh...
	 * @param secondaryTables Any secondary tables available.
	 *
	 * @return The index column
	 */
	public static IndexColumn buildColumnFromAnnotation(
			OrderColumn ann,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext buildingContext) {
		if ( ann != null ) {
			final String sqlType = isEmptyAnnotationValue( ann.columnDefinition() ) ? null : ann.columnDefinition();
			final String name = isEmptyAnnotationValue( ann.name() ) ? inferredData.getPropertyName() + "_ORDER" : ann.name();
			//TODO move it to a getter based system and remove the constructor
			final IndexColumn column = new IndexColumn();
			column.setLogicalColumnName( name );
			column.setSqlType( sqlType );
			column.setNullable( ann.nullable() );
			column.setJoins( secondaryTables );
			column.setInsertable( ann.insertable() );
			column.setUpdatable( ann.updatable() );
			column.setBuildingContext( buildingContext );
			column.setPropertyHolder( propertyHolder );
			column.bind();
			return column;
		}
		else {
			final IndexColumn column = new IndexColumn();
			column.setImplicit( true );
			column.setBuildingContext( buildingContext );
			column.setPropertyHolder( propertyHolder );
			column.bind();
			return column;
		}
	}

	/**
	 * Legacy {@link IndexColumn @IndexColumn} processing.
	 *
	 * @param ann The IndexColumn annotation instance
	 * @param propertyHolder Information about the property
	 * @param inferredData Yeah, right.  Uh...
	 *
	 * @return The index column
	 */
	public static IndexColumn buildColumnFromAnnotation(
			org.hibernate.annotations.IndexColumn ann,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			MetadataBuildingContext buildingContext) {
		if ( ann != null ) {
			final String sqlType = isEmptyAnnotationValue( ann.columnDefinition() ) ? null : ann.columnDefinition();
			final String name = isEmptyAnnotationValue( ann.name() ) ? inferredData.getPropertyName() : ann.name();
			//TODO move it to a getter based system and remove the constructor
			final IndexColumn column = new IndexColumn();
			column.setLogicalColumnName( name );
			column.setSqlType( sqlType );
			column.setNullable( ann.nullable() );
			column.setBase( ann.base() );
			column.setBuildingContext( buildingContext );
			column.setPropertyHolder( propertyHolder );
			column.bind();
			return column;
		}
		else {
			final IndexColumn column = new IndexColumn();
			column.setImplicit( true );
			column.setBuildingContext( buildingContext );
			column.setPropertyHolder( propertyHolder );
			column.bind();
			return column;
		}
	}
}
