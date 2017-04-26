/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi.criteria.from;

import java.util.Map;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Predicate;

/**
 * @author Steve Ebersole
 */
public interface JpaMapJoin<Z,K,V> extends JpaPluralAttributeJoin<Z,Map<K,V>,V>, MapJoin<Z,K,V> {
	@Override
	JpaMapJoin<Z,K,V> on(Expression<Boolean> restriction);

	@Override
	JpaMapJoin<Z,K,V> on(Predicate... restrictions);
}
