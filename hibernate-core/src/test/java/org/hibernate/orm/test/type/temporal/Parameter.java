package org.hibernate.orm.test.type.temporal;

/**
 * @author Steve Ebersole
 */
public record Parameter<V,D extends Data<V>>(Environment env, D data) {
}
