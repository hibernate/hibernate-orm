package org.hibernate.spatial.dialect.sqlserver.convertors;

import com.vividsolutions.jts.geom.Geometry;

import java.io.Serializable;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 1/11/12
 */
public class ConvertorTestData implements Serializable {
    public Integer id;
    public String type;
    public Geometry geometry;
    public byte[] bytes;
}
