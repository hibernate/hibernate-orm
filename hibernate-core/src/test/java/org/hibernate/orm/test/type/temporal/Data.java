package org.hibernate.orm.test.type.temporal;

/**
 * @author Steve Ebersole
 */
public interface Data<V> {
	V makeValue();
}
