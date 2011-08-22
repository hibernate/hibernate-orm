package org.hibernate.spatial.dialect.oracle;

import java.sql.SQLException;
import java.sql.Struct;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Jul 1, 2010
 */
class SDOPoint {
    public double x = 0.0;

    public double y = 0.0;

    public double z = Double.NaN;

    public SDOPoint(Struct struct) {
        try {
            Object[] data = struct.getAttributes();
            this.x = ((Number) data[0]).doubleValue();
            this.y = ((Number) data[1]).doubleValue();
            if (data[2] != null) {
                this.z = ((Number) data[1]).doubleValue();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String toString() {
        StringBuilder stb = new StringBuilder();
        stb.append("(").append(x).append(",").append(y).append(",").append(
                z).append(")");
        return stb.toString();
    }

}