package org.hibernate.spatial;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 7/27/11
 */
public class SpatialGeometryJavaTypeDescriptor extends AbstractTypeDescriptor<Geometry> {


    public static final JavaTypeDescriptor<Geometry> INSTANCE = new SpatialGeometryJavaTypeDescriptor(Geometry.class);

    protected SpatialGeometryJavaTypeDescriptor(Class<Geometry> type) {
        super(type);
    }

    @Override
    public String toString(Geometry value) {
        return value.toText();
    }

    @Override
    public Geometry fromString(String string) {
        WKTReader reader = new WKTReader();
        try {
            return reader.read(string);
        } catch (ParseException e) {
            throw new RuntimeException(String.format("Can't parse string %s as WKT",string));
        }
    }

    @Override
    public <X> X unwrap(Geometry value, Class<X> type, WrapperOptions options) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <X> Geometry wrap(X value, WrapperOptions options) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
