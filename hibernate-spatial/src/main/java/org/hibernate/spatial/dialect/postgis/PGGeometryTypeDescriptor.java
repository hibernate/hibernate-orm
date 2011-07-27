package org.hibernate.spatial.dialect.postgis;

import java.sql.Types;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 7/27/11
 */
public class PGGeometryTypeDescriptor implements SqlTypeDescriptor {


	public static final SqlTypeDescriptor INSTANCE = new PGGeometryTypeDescriptor();

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
		return (ValueBinder<X>) new PGGeometryValueBinder();
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return (ValueExtractor<X>) new PGGeometryValueExtractor();
	}
}
