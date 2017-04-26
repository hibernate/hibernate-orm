/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi.criteria.from;

import java.util.List;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Predicate;

/**
 * @author Steve Ebersole
 */
public interface JpaListJoin<Z,E> extends JpaPluralAttributeJoin<Z,List<E>,E>, ListJoin<Z,E> {
	@Override
	JpaListJoin<Z, E> on(Expression<Boolean> restriction);

	@Override
	JpaListJoin<Z, E> on(Predicate... restrictions);
}
