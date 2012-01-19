package org.hibernate.spatial.dialect.mysql;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import java.sql.Types;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 1/17/12
 */
public class MySQLGeometryTypeDescriptor implements SqlTypeDescriptor {

    public static final MySQLGeometryTypeDescriptor INSTANCE = new MySQLGeometryTypeDescriptor();

    @Override
    public int getSqlType() {
        return Types.ARRAY;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean canBeRemapped() {
        return false;
    }

    @Override
    public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
        return (ValueBinder<X>) new MySQLGeometryValueBinder();
    }

    @Override
    public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
        return (ValueExtractor<X>) new MySQLGeometryValueExtractor();
    }
}
