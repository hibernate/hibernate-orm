package org.hibernate.sql.ast.spi.query.predicate;

/**
 * Something that can contain predicates
 *
 * @author Steve Ebersole
 */
public interface PredicateContainer {
	/**
	 * Apply a predicate to this container
	 */
	void applyPredicate(Predicate predicate);
}
