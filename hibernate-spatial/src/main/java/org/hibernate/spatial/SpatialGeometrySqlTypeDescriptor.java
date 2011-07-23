package org.hibernate.spatial;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import java.sql.Types;

/**
 * A generic <code>SqlTypeDescriptor</code>, intended to be remapped
 * by the spatial dialect.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 7/27/11
 */
public class SpatialGeometrySqlTypeDescriptor implements SqlTypeDescriptor {

    public static final SpatialGeometrySqlTypeDescriptor INSTANCE = new SpatialGeometrySqlTypeDescriptor();

    @Override
    public int getSqlType() {
        return Types.STRUCT; //this works only for postgis!
        //sqltype remapping issue: HHH-6074 needs to be resolved first.
//        return Types.OTHER;

    }

    @Override
    public boolean canBeRemapped() {
        return true;
    }

    @Override
    public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
        throw new UnsupportedOperationException();
    }
}
