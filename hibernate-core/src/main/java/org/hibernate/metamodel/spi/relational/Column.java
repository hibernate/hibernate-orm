/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.spi.relational;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.sql.Template;

/**
 * Models a physical column
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class Column extends AbstractValue {
	public static final int DEFAULT_LENGTH = 255;
	public static final int DEFAULT_PRECISION = 19;
	public static final int DEFAULT_SCALE = 2;
	
	private final Identifier columnName;
	private boolean nullable = true; 

	private String defaultValue;
	private String checkCondition;
	private String sqlType;

	private String readFragment;
	private String writeFragment;

	private String comment;

	private Size size = new Size();

	private boolean isIdentity = false;

	protected Column(int position, Identifier name) {
		super( position );
		this.columnName = name;
	}

	public Identifier getColumnName() {
		return columnName;
	}

	public boolean isNullable() {
		return nullable;
	}

	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getCheckCondition() {
		return checkCondition;
	}

	public void setCheckCondition(String checkCondition) {
		this.checkCondition = checkCondition;
	}

	public String getSqlType() {
        return sqlType;
	}

	public void setSqlType(String sqlType) {
		this.sqlType = sqlType;
	}

	// TODO: Solely used by schema tools.  Re-think this, getSqlType, and getJdbcDataType.  Clean-up and/or
	// condense somehow?
	public String getSqlTypeString(Dialect dialect) {
		if ( sqlType != null ) {
			return sqlType;
		}
		else {
			return dialect.getTypeName(
					getJdbcDataType().getTypeCode(),
					size.getLength(),
					size.getPrecision(),
					size.getScale()
			);
		}
	}

	public String getReadFragment() {
		return readFragment;
	}

	public void setReadFragment(String readFragment) {
		this.readFragment = readFragment;
	}

	public String getWriteFragment() {
		return writeFragment;
	}

	public void setWriteFragment(String writeFragment) {
		this.writeFragment = writeFragment;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getTemplate(Dialect dialect, SQLFunctionRegistry functionRegistry) {
		return hasCustomRead()
				? Template.renderWhereStringTemplate( readFragment, dialect, functionRegistry )
				: Template.TEMPLATE + '.' + getColumnName().getText( dialect );
	}

	public boolean hasCustomRead() {
		return StringHelper.isNotEmpty( readFragment );
	}

	public String getReadExpr(Dialect dialect) {
		return hasCustomRead() ? readFragment : getColumnName().getText( dialect );
	}

	public String getWriteExpr() {
		return StringHelper.isNotEmpty( writeFragment ) ? writeFragment : "?";
	}


	public Size getSize() {
		return size;
	}

	public void setSize(Size size) {
		this.size = size;
	}

	/**
	 * Returns true if this column is an identity column.
	 * @return true, if this column is an identity column; false, otherwise.
	 */
	public boolean isIdentity() {
		return isIdentity;
	}

	/**
	 * Indicate if this column is an identity column.
	 * @param isIdentity - true, if this column is an identity column; false, otherwise.
	 */
	public void setIdentity(boolean isIdentity) {
		this.isIdentity = isIdentity;
	}

	@Override
	public ValueType getValueType() {
		return ValueType.COLUMN;
	}

	@Override
	public String toLoggableString() {
		return getColumnName().getText();
	}

	@Override
	public String getAlias(Dialect dialect, TableSpecification tableSpecification) {
		if ( tableSpecification == null ) {
			// see HHH-7547 -- protect against ambiguity
			throw new IllegalArgumentException("To ensure uniqueness, tableSpecification must not be null");
		}
		
		final int lastLetter = StringHelper.lastIndexOfLetter( columnName.getText() );
		final String colPositionSuffix = String.valueOf( getPosition() ) + '_';
		final String tableNumberSuffix = String.valueOf( tableSpecification.getTableNumber() ) + "_";
		final String suffix = colPositionSuffix + tableNumberSuffix;

		String alias;
		if ( lastLetter == -1 ) {
			alias = "column" ;
		}
		else if ( columnName.getText().length() > lastLetter + 1 ) {
			alias = columnName.getText().substring( 0, lastLetter + 1 );
		}
		else {
			alias = columnName.getText();
		}

		if ( alias.length() + suffix.length() > dialect.getMaxAliasLength() ) {
			alias = alias.substring( 0, dialect.getMaxAliasLength() - suffix.length() );
		}
		
		return alias + suffix;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		Column column = (Column) o;

		if ( columnName != null ? !columnName.equals( column.columnName ) : column.columnName != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return columnName != null ? columnName.hashCode() : 0;
	}
}
