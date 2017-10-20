/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import org.hibernate.dialect.Dialect;
import org.hibernate.naming.Identifier;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class PhysicalColumn implements Column {
	private final Table table;
	private final Identifier name;

	private final SqlTypeDescriptor sqlTypeDescriptor;
	private Size size;
	private String sqlType;

	private final String defaultValue;
	private String checkConstraint;
	private final boolean isNullable;
	private final boolean isUnique;
	private final String comment;

	private String customReadExpr;
	private String customWriteExpr;

	public PhysicalColumn(
			Table table,
			Identifier name,
			SqlTypeDescriptor sqlTypeDescriptor,
			String defaultValue,
			String sqlType,
			boolean isNullable,
			boolean isUnique) {
		this( table, name, sqlTypeDescriptor, defaultValue, sqlType, isNullable, isUnique, null );
	}

	public PhysicalColumn(
			Table table,
			Identifier name,
			SqlTypeDescriptor sqlTypeDescriptor,
			String defaultValue,
			String sqlType,
			boolean isNullable,
			boolean isUnique,
			String comment) {
		this.table = table;
		this.name = name;
		this.sqlTypeDescriptor = sqlTypeDescriptor;
		this.defaultValue = defaultValue;
		this.sqlType = sqlType;
		this.isNullable = isNullable;
		this.isUnique = isUnique;
		this.comment = comment;
	}

	public Identifier getName() {
		return name;
	}

	@Override
	public Table getSourceTable() {
		return table;
	}

	@Override
	public String getExpression() {
		return name.getCanonicalName();
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return sqlTypeDescriptor;
	}

	@Override
	public String render(String identificationVariable) {
		return identificationVariable + '.' + name;
	}

	@Override
	public String toString() {
		return "PhysicalColumn(" + table.getTableExpression() + " : " + name + ")";
	}

	@Override
	public String toLoggableString() {
		return "PhysicalColumn(" + name + ");";
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public boolean isNullable() {
		return isNullable;
	}

	public boolean isUnique() {
		return isUnique;
	}

	public String getComment() {
		return comment;
	}

	public Size getSize() {
		return size;
	}

	public void setSize(Size size) {
		this.size = size;
	}

	public void setCheckConstraint(String checkConstraint) {
		this.checkConstraint = checkConstraint;
	}

	public String getSqlTypeName(Dialect dialect) {
		if ( sqlType == null ) {
			Size size = getSize();
			if ( size.getLength() == null ) {
				size = dialect.getDefaultSizeStrategy().resolveDefaultSize( getSqlTypeDescriptor(), null );
			}
			sqlType = dialect.getTypeName( getSqlTypeDescriptor().getJdbcTypeCode(), size );
		}
		return sqlType;
	}

	public String getCheckConstraint() {
		return checkConstraint;
	}
}
