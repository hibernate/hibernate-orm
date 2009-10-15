/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cfg;

import java.util.Map;

import javax.persistence.OrderColumn;

import org.hibernate.mapping.Join;

/**
 * index column
 *
 * @author inger
 */
public class IndexColumn
		extends Ejb3Column {

	private int base;

	//FIXME move to a getter setter strategy for readeability
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
			ExtendedMappings mappings
	) {
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
		setSecondaryTableName( secondaryTableName );
		setPropertyHolder( propertyHolder );
		setJoins( joins );
		setMappings( mappings );
		bind();
		//super(isImplicit, sqlType, length, precision, scale, name, nullable, unique, insertable, updatable, secondaryTableName, joins, propertyHolder, mappings);

	}

	public int getBase() {
		return base;
	}

	public void setBase(int base) {
		this.base = base;
	}

	//JPA 2 @OrderColumn processing
	public static IndexColumn buildColumnFromAnnotation(
			OrderColumn ann,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Map<String, Join> secondaryTables,
			ExtendedMappings mappings
	) {
		IndexColumn column;
		if ( ann != null ) {
			String sqlType = BinderHelper.isDefault( ann.columnDefinition() ) ? null : ann.columnDefinition();
			String name = BinderHelper.isDefault( ann.name() ) ? inferredData.getPropertyName() + "_ORDER" : ann.name();
			//TODO move it to a getter based system and remove the constructor
// The JPA OrderColumn annotation defines no table element...
//			column = new IndexColumn(
//					false, sqlType, 0, 0, 0, name, ann.nullable(),
//					false, ann.insertable(), ann.updatable(), ann.table(),
//					secondaryTables, propertyHolder, mappings
//			);
			column = new IndexColumn(
					false, sqlType, 0, 0, 0, name, ann.nullable(),
					false, ann.insertable(), ann.updatable(), /*ann.table()*/null,
					secondaryTables, propertyHolder, mappings
			);
		}
		else {
			column = new IndexColumn(
					true, null, 0, 0, 0, null, true,
					false, true, true, null, null, propertyHolder, mappings
			);
		}
		return column;
	}

	//legacy @IndexColumn processing
	public static IndexColumn buildColumnFromAnnotation(
			org.hibernate.annotations.IndexColumn ann,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			ExtendedMappings mappings
	) {
		IndexColumn column;
		if ( ann != null ) {
			String sqlType = BinderHelper.isDefault( ann.columnDefinition() ) ? null : ann.columnDefinition();
			String name = BinderHelper.isDefault( ann.name() ) ? inferredData.getPropertyName() : ann.name();
			//TODO move it to a getter based system and remove the constructor
			column = new IndexColumn(
					false, sqlType, 0, 0, 0, name, ann.nullable(),
					false, true, true, null, null, propertyHolder, mappings
			);
			column.setBase( ann.base() );
		}
		else {
			column = new IndexColumn(
					true, null, 0, 0, 0, null, true,
					false, true, true, null, null, propertyHolder, mappings
			);
		}
		return column;
	}
}
