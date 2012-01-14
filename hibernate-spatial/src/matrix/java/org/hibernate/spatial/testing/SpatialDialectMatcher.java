package org.hibernate.spatial.testing;

import org.hibernate.dialect.Dialect;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.testing.Skip;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 1/13/12
 */
public class SpatialDialectMatcher implements Skip.Matcher {
    @Override
    public boolean isMatch() {
        return !(Dialect.getDialect() instanceof SpatialDialect);
    }
}
