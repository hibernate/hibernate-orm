/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm;

import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.spi.NavigablePath;

/**
 * Specialization for attributes that that can be used in creating SQM joins
 *
 * todo (6.0) : should we define this for entities as well to handle cross joins and "entity joins"?
 * 		- the result type would need to change to just SqmJoin...
 *
 * @author Steve Ebersole
 */
public interface SqmJoinable<O, E> {
	SqmJoin<O, E> createSqmJoin(
			SqmFrom<?, O> lhs,
			SqmJoinType joinType,
			String alias,
			boolean fetched,
			SqmCreationState creationState);

	String getName();

	default NavigablePath createNavigablePath(SqmPath<?> parent, String alias) {
		return SqmCreationHelper.buildSubNavigablePath( parent, getName(), alias );
	}
}
