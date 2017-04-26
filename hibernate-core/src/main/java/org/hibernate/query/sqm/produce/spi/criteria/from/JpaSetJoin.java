/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi.criteria.from;

import java.util.Set;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.SetJoin;

/**
 * @author Steve Ebersole
 */
public interface JpaSetJoin<Z, E> extends JpaPluralAttributeJoin<Z,Set<E>, E>, SetJoin<Z, E> {
	@Override
	JpaSetJoin<Z, E> on(Expression<Boolean> restriction);

	@Override
	JpaSetJoin<Z, E> on(Predicate... restrictions);
}
