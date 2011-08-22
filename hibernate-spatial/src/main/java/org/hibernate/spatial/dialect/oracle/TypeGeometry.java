package org.hibernate.spatial.dialect.oracle;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Jul 1, 2010
 */
enum TypeGeometry {

    UNKNOWN_GEOMETRY(0), POINT(1), LINE(2), POLYGON(3), COLLECTION(4), MULTIPOINT(
            5), MULTILINE(6), MULTIPOLYGON(7), SOLID(8), MULTISOLID(9);

    private int gtype = 0;

    TypeGeometry(int gtype) {
        this.gtype = gtype;
    }

    int intValue() {
        return this.gtype;
    }

    static TypeGeometry parse(int v) {
        for (TypeGeometry gt : values()) {
            if (gt.intValue() == v) {
                return gt;
            }
        }
        throw new RuntimeException("Value " + v
                + " isn't a valid TypeGeometry value");
    }

}