/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria;

import org.hibernate.metamodel.model.domain.EntityDomainType;

/**
 * @author Steve Ebersole
 */
public interface JpaTreatedJoin<L,R,R1 extends R> extends JpaTreatedFrom<L,R,R1>, JpaJoin<L,R1> {
	@Override
	<S extends R1> JpaTreatedJoin<L, R1, S> treatAs(Class<S> treatJavaType);

	@Override
	<S extends R1> JpaTreatedJoin<L, R1, S> treatAs(EntityDomainType<S> treatJavaType);
}
