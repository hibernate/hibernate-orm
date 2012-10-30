package org.hibernate.spatial.dialect.sqlserver.convertors;

import org.geolatte.geom.*;
import org.geolatte.geom.crs.CrsId;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 10/19/12
 */
public class CountingPointSequenceBuilder implements PointSequenceBuilder {

    final private PointSequenceBuilder delegate;
    private int num = 0;

    public CountingPointSequenceBuilder(DimensionalFlag df, CrsId crsId) {
        delegate = PointSequenceBuilders.variableSized(df, crsId);
    }

    @Override
    public PointSequenceBuilder add(double[] coordinates) {
        num++;
        return delegate.add(coordinates);
    }

    @Override
    public PointSequenceBuilder add(double x, double y) {
        num++;
        return delegate.add(x, y);
    }

    @Override
    public PointSequenceBuilder add(double x, double y, double zOrm) {
        num++;
        return delegate.add(x, y, zOrm);
    }

    @Override
    public PointSequenceBuilder add(double x, double y, double z, double m) {
        num++;
        return delegate.add(x, y, z, m);
    }

    @Override
    public PointSequenceBuilder add(Point pnt) {
        num++;
        return delegate.add(pnt);
    }

    @Override
    public DimensionalFlag getDimensionalFlag() {
        return delegate.getDimensionalFlag();
    }

	@Override
	public CrsId getCrsId() {
		return delegate.getCrsId();
	}

	@Override
    public PointSequence toPointSequence() {
        return delegate.toPointSequence();
    }

    public int getNumAdded(){
        return num;
    }
}
