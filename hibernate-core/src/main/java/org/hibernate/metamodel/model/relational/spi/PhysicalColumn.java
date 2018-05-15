/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import java.util.Objects;
import java.util.function.Supplier;

import org.hibernate.dialect.Dialect;
import org.hibernate.naming.Identifier;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class PhysicalColumn implements Column {
	private final Table table;
	private final Identifier name;

	private final Supplier<SqlTypeDescriptor> sqlTypeDescriptorAccess;
	private Size size;
	private String sqlType;

	private Supplier<BasicJavaDescriptor> javaTypeDescriptorAccess;

	private SqlExpressableType sqlExpressableType;

	private final String defaultValue;
	private String checkConstraint;
	private final boolean isNullable;
	private final boolean isUnique;
	private final String comment;
	private final TypeConfiguration typeConfiguration;
	private Dialect dialect;

	public PhysicalColumn(
			Table table,
			Identifier name,
			Supplier<SqlTypeDescriptor> sqlTypeDescriptorAccess,
			Supplier<BasicJavaDescriptor> javaTypeDescriptorAccess,
			String defaultValue,
			String sqlType,
			boolean isNullable,
			boolean isUnique,
			TypeConfiguration typeConfiguration) {
		this(
				table,
				name,
				sqlTypeDescriptorAccess,
				javaTypeDescriptorAccess,
				defaultValue,
				sqlType,
				isNullable,
				isUnique,
				null,
				typeConfiguration
		);
	}

	public PhysicalColumn(
			Table table,
			Identifier name,
			Supplier<SqlTypeDescriptor> sqlTypeDescriptorAccess,
			Supplier<BasicJavaDescriptor> javaTypeDescriptorAccess,
			String defaultValue,
			String sqlType,
			boolean isNullable,
			boolean isUnique,
			String comment,
			TypeConfiguration typeConfiguration) {
		this.table = table;
		this.name = name;
		this.sqlTypeDescriptorAccess = sqlTypeDescriptorAccess;
		this.javaTypeDescriptorAccess = javaTypeDescriptorAccess;
		this.defaultValue = defaultValue;
		this.sqlType = sqlType;
		this.isNullable = isNullable;
		this.isUnique = isUnique;
		this.comment = comment;
		this.typeConfiguration = typeConfiguration;
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
		return name.getText();
	}

	@Override
	public SqlExpressableType getExpressableType() {
		if ( sqlExpressableType == null ) {
			sqlExpressableType = getSqlTypeDescriptor().getSqlExpressableType(
					getJavaTypeDescriptor(),
					typeConfiguration
			);
		}

		return sqlExpressableType;
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return sqlTypeDescriptorAccess.get();
	}

	@Override
	public BasicJavaDescriptor getJavaTypeDescriptor() {
		return javaTypeDescriptorAccess.get();
	}

	public Supplier<SqlTypeDescriptor> getSqlTypeDescriptorAccess() {
		return sqlTypeDescriptorAccess;
	}

	public Supplier<BasicJavaDescriptor> getJavaTypeDescriptorAccess() {
		return javaTypeDescriptorAccess;
	}

	@Override
	public String render(String identificationVariable) {
		if ( dialect == null ) {
			dialect = typeConfiguration.getSessionFactory().getJdbcServices().getDialect();
		}
		if ( identificationVariable != null ) {
			return identificationVariable + '.' + render( dialect );
		}
		return render( dialect );
	}

	@Override
	public String render() {
		if ( dialect == null ) {
			dialect = typeConfiguration.getSessionFactory().getJdbcServices().getDialect();
		}
		return render( dialect );
	}

	private String render(Dialect dialect){
		return name.render( dialect );
	}

	@Override
	public String toString() {
		return "PhysicalColumn(" + table.getTableExpression() + " : " + name + ")";
	}

	@Override
	public String toLoggableString() {
		return toString();
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

	public String getSqlTypeName() {
		return sqlType;
	}

	public String getCheckConstraint() {
		return checkConstraint;
	}

	public TypeConfiguration getTypeConfiguration(){
		return typeConfiguration;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		PhysicalColumn that = (PhysicalColumn) o;
		return Objects.equals( table, that.table ) &&
				Objects.equals( name, that.name );
	}

	@Override
	public int hashCode() {
		return Objects.hash( table, name );
	}
}
