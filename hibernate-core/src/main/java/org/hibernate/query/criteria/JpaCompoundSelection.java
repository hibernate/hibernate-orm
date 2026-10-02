package org.hibernate.query.criteria;

import jakarta.persistence.criteria.CompoundSelection;

/**
 * @author Steve Ebersole
 */
public interface JpaCompoundSelection<T> extends JpaSelection<T>, CompoundSelection<T> {
}
