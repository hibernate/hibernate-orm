package org.hibernate.spatial.dialect.oracle;

import java.sql.Types;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 8/22/11
 */
public class SDOGeometryTypeDescriptor implements SqlTypeDescriptor {

	public static SDOGeometryTypeDescriptor INSTANCE = new SDOGeometryTypeDescriptor();

	@Override
	public int getSqlType() {
		return Types.STRUCT;
	}

	@Override
	public boolean canBeRemapped() {
		return false;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return (ValueBinder<X>) new SDOGeometryValueBinder();
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return (ValueExtractor<X>) new SDOGeometryValueExtractor();
	}


}
