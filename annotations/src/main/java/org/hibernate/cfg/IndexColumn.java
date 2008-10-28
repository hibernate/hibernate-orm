package org.hibernate.cfg;

import java.util.Map;

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
