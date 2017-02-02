/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;
import java.util.Map;
import javax.persistence.OrderColumn;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Join;

/**
 * index column
 *
 * @author inger
 */
public class IndexColumn extends Ejb3Column {
	private int base;

	// TODO move to a getter setter strategy for readability
	public IndexColumn(
			boolean isImplicit,
			String sqlType,
			int length,
			int precision,
			int scale,
			String name,
			boolean nullable,
			boolean unique,
			boolean insertable,
			boolean updatable,
			String secondaryTableName,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			MetadataBuildingContext buildingContext) {
		super();
		setImplicit( isImplicit );
		setSqlType( sqlType );
		setLength( length );
		setPrecision( precision );
		setScale( scale );
		setLogicalColumnName( name );
		setNullable( nullable );
		setUnique( unique );
		setInsertable( insertable );
		setUpdatable( updatable );
		setExplicitTableName( secondaryTableName );
		setPropertyHolder( propertyHolder );
		setJoins( joins );
		setBuildingContext( buildingContext );
		bind();
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
		final IndexColumn column;
		if ( ann != null ) {
			final String sqlType = BinderHelper.isEmptyAnnotationValue( ann.columnDefinition() ) ? null : ann.columnDefinition();
			final String name = BinderHelper.isEmptyAnnotationValue( ann.name() ) ? inferredData.getPropertyName() + "_ORDER" : ann.name();
			//TODO move it to a getter based system and remove the constructor
// The JPA OrderColumn annotation defines no table element...
//			column = new IndexColumn(
//					false, sqlType, 0, 0, 0, name, ann.nullable(),
//					false, ann.insertable(), ann.updatable(), ann.table(),
//					secondaryTables, propertyHolder, mappings
//			);
			column = new IndexColumn(
					false,
					sqlType,
					0,
					0,
					0,
					name,
					ann.nullable(),
					false,
					ann.insertable(),
					ann.updatable(),
					/*ann.table()*/null,
					secondaryTables,
					propertyHolder,
					buildingContext
			);
		}
		else {
			column = new IndexColumn(
					true,
					null,
					0,
					0,
					0,
					null,
					true,
					false,
					true,
					true,
					null,
					null,
					propertyHolder,
					buildingContext
			);
		}
		return column;
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
		final IndexColumn column;
		if ( ann != null ) {
			final String sqlType = BinderHelper.isEmptyAnnotationValue( ann.columnDefinition() ) ? null : ann.columnDefinition();
			final String name = BinderHelper.isEmptyAnnotationValue( ann.name() ) ? inferredData.getPropertyName() : ann.name();
			//TODO move it to a getter based system and remove the constructor
			column = new IndexColumn(
					false,
					sqlType,
					0,
					0,
					0,
					name,
					ann.nullable(),
					false,
					true,
					true,
					null,
					null,
					propertyHolder,
					buildingContext
			);
			column.setBase( ann.base() );
		}
		else {
			column = new IndexColumn(
					true,
					null,
					0,
					0,
					0,
					null,
					true,
					false,
					true,
					true,
					null,
					null,
					propertyHolder,
					buildingContext
			);
		}
		return column;
	}
}
