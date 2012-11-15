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

import org.hibernate.MappingException;
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
	private final Identifier columnName;
	private boolean nullable = true;
	private boolean unique;

	private String defaultValue;
	private String checkCondition;
	private String sqlType;

	private String readFragment;
	private String writeFragment;

	private String comment;

	private Size size = new Size();

	private boolean isIdentity = false;

	protected Column(TableSpecification table, int position, String name) {
		this( table, position, Identifier.toIdentifier( name ) );
	}

	protected Column(TableSpecification table, int position, Identifier name) {
		super( table, position );
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

	public boolean isUnique() {
		return unique;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
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
	public String toLoggableString() {
		return getTable().getLoggableValueQualifier() + '.' + getColumnName();
	}

	// TODO: this is fairly complicated logic. It would be more straightforward
	// to always include the column position and a table number in the unique
	// suffix. That might cause unreadable aliases to be generated more often though...
	@Override
	public String getAlias(Dialect dialect, TableSpecification tableSpecification) {
		final int lastLetter = StringHelper.lastIndexOfLetter( columnName.getText() );
		final String colPositionSuffix = String.valueOf( getPosition() ) + '_';
		final String tableNumberSuffix =
				tableSpecification == null ?
						"" :
						String.valueOf( tableSpecification.getTableNumber() ) + "_";

		String alias;
		String suffix = colPositionSuffix + tableNumberSuffix;
		boolean useRawName = false;
		if ( lastLetter == -1 ) {
			alias = "column" ;
		}
		else if ( columnName.getText().length() > lastLetter + 1 ) {
			alias = columnName.getText().substring( 0, lastLetter + 1 );
		}
		else {
			alias = columnName.getText();
			if (columnName.getText().length() + tableNumberSuffix.length() <= dialect.getMaxAliasLength() &&
							! columnName.isQuoted() &&
							! columnName.getText().toLowerCase().equals( "rowid" ) ) {
				useRawName = true;
				suffix = tableNumberSuffix;
			}
		}

		if ( ! useRawName ) {
			if ( suffix.length() >= dialect.getMaxAliasLength() ) {
				throw new MappingException(
						String.format( "Unique suffix [%s%s] length must be less than maximum [%d]",
								colPositionSuffix,
								tableNumberSuffix,
								dialect.getMaxAliasLength()
						)
				);
			}
			if ( alias.length() + suffix.length() > dialect.getMaxAliasLength() ) {
				alias = alias.substring( 0, dialect.getMaxAliasLength() - suffix.length() );
			}
		}
		return alias + suffix;
	}
}
