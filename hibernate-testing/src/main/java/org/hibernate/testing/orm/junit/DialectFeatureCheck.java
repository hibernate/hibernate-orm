package org.hibernate.testing.orm.junit;

import org.hibernate.dialect.Dialect;


/**
 * @author Andrea Boriero
 */
@FunctionalInterface
public interface DialectFeatureCheck {
	boolean apply(Dialect dialect);
}
