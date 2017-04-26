/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi.criteria.from;

import java.util.Collection;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

/**
 * @author Steve Ebersole
 */
public interface JpaCollectionJoin<Z,E> extends JpaPluralAttributeJoin<Z,Collection<E>,E>, CollectionJoin<Z,E> {
	@Override
	JpaCollectionJoin<Z, E> on(Expression<Boolean> restriction);

	@Override
	JpaCollectionJoin<Z, E> on(Predicate... restrictions);
}
