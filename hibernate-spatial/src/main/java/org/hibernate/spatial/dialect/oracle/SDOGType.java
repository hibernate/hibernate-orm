package org.hibernate.spatial.dialect.oracle;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Jun 30, 2010
 */
class SDOGType {

    private int dimension = 2;

    private int lrsDimension = 0;

    private TypeGeometry typeGeometry = TypeGeometry.UNKNOWN_GEOMETRY;

    public SDOGType(int dimension, int lrsDimension,
                    TypeGeometry typeGeometry) {
        setDimension(dimension);
        setLrsDimension(lrsDimension);
        setTypeGeometry(typeGeometry);
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        if (dimension < 2 || dimension > 4) {
            throw new IllegalArgumentException(
                    "Dimension can only be 2,3 or 4.");
        }
        this.dimension = dimension;
    }

    public TypeGeometry getTypeGeometry() {
        return typeGeometry;
    }

    public void setTypeGeometry(TypeGeometry typeGeometry) {

        this.typeGeometry = typeGeometry;
    }

    public int getLRSDimension() {
        if (this.lrsDimension > 0) {
            return this.lrsDimension;
        } else if (this.lrsDimension == 0 && this.dimension == 4) {
            return 4;
        }
        return 0;
    }

    public int getZDimension() {
        if (this.dimension > 2) {
            if (!isLRSGeometry()) {
                return this.dimension;
            } else {
                return (getLRSDimension() < this.dimension ? 4 : 3);
            }
        }
        return 0;
    }

    public boolean isLRSGeometry() {
        return (this.lrsDimension > 0 || (this.lrsDimension == 0 && this.dimension == 4));
    }

    public void setLrsDimension(int lrsDimension) {
        if (lrsDimension != 0 && lrsDimension > this.dimension) {
            throw new IllegalArgumentException(
                    "lrsDimension must be 0 or lower or equal to dimenstion.");
        }
        this.lrsDimension = lrsDimension;
    }

    public int intValue() {
        int v = this.dimension * 1000;
        v += lrsDimension * 100;
        v += typeGeometry.intValue();
        return v;
    }

    public static SDOGType parse(int v) {
        int dim = v / 1000;
        v -= dim * 1000;
        int lrsDim = v / 100;
        v -= lrsDim * 100;
        TypeGeometry typeGeometry = TypeGeometry.parse(v);
        return new SDOGType(dim, lrsDim, typeGeometry);
    }

    public static SDOGType parse(Object datum) {

        try {
            int v = ((Number) datum).intValue();
            return parse(v);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public String toString() {
        return Integer.toString(this.intValue());
    }
}