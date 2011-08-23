package org.hibernate.spatial.dialect.sqlserver;

import java.sql.Types;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 8/23/11
 */
public class SqlServer2008GeometryTypeDescriptor implements SqlTypeDescriptor {

	public static final SqlTypeDescriptor INSTANCE = new SqlServer2008GeometryTypeDescriptor();

	@Override
	public int getSqlType() {
		return Types.ARRAY;
	}

	@Override
	public boolean canBeRemapped() {
		return false;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return (ValueBinder<X>) new SqlServer2008GeometryValueBinder();
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return (ValueExtractor<X>) new SqlServer2008GeometryValueExtractor();
	}

}
