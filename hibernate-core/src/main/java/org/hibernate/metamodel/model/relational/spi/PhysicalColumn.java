/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import org.hibernate.dialect.Dialect;
import org.hibernate.naming.Identifier;
import org.hibernate.sql.results.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.results.spi.SqlSelectionReader;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class PhysicalColumn implements Column {
	private final Table table;
	private final Identifier name;
	private final SqlTypeDescriptor sqlTypeDescriptor;
	private final String defaultValue;
	private final boolean isNullable;
	private final boolean isUnique;
	private final String comment;
	private String sqlType;
	private String checkConstraint;
	private int length;
	private int precision;
	private int scale;

	private final SqlSelectionReader sqlSelectionReader;

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

		this.sqlSelectionReader = new SqlSelectionReaderImpl( sqlTypeDescriptor.getJdbcTypeCode() );
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

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getPrecision() {
		return precision;
	}

	public void setPrecision(int precision) {
		this.precision = precision;
	}

	public int getScale() {
		return scale;
	}

	public void setScale(int scale) {
		this.scale = scale;
	}

	public void setCheckConstraint(String checkConstraint) {
		this.checkConstraint = checkConstraint;
	}

	public String getSqlType(Dialect dialect) {
		if ( sqlType == null ) {
			sqlType = dialect.getTypeName(
					getSqlTypeDescriptor().getJdbcTypeCode(),
					getLength(),
					getPrecision(),
					getScale()
			);
		}
		return sqlType;
	}

	public String getCheckConstraint() {
		return checkConstraint;
	}
}
