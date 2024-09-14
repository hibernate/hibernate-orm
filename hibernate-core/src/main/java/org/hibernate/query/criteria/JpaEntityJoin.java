/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.tree.domain.SqmTreatedEntityJoin;

/**
 * @author Steve Ebersole
 */
public interface JpaEntityJoin<L,R> extends JpaJoin<L,R> {
	@Override
	EntityDomainType<R> getModel();

	@Override
	<S extends R> SqmTreatedEntityJoin<L,R,S> treatAs(Class<S> treatAsType);

	@Override
	<S extends R> SqmTreatedEntityJoin<L,R,S> treatAs(EntityDomainType<S> treatAsType);
}
