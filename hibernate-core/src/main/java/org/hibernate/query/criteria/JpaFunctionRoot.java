package org.hibernate.query.criteria;

import org.hibernate.Incubating;

/**
 * @since 7.0
 */
@Incubating(since = "6.3")
public interface JpaFunctionRoot<E> extends JpaFunctionFrom<E, E>, JpaRoot<E> {

}
