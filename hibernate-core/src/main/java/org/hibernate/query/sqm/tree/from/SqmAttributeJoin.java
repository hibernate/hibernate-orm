/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.Internal;
import org.hibernate.Remove;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaFetch;
import org.hibernate.query.criteria.JpaJoin;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Models a join based on a mapped attribute reference.
 *
 * @author Steve Ebersole
 */
public interface SqmAttributeJoin<O,T> extends SqmQualifiedJoin<O,T>, JpaFetch<O,T>, JpaJoin<O,T> {
	@Override
	SqmFrom<?,O> getLhs();

	@Override
	default boolean isImplicitlySelectable() {
		return !isFetched();
	}

	@Override
	SqmPathSource<T> getReferencedPathSource();

	@Override
	JavaType<T> getJavaTypeDescriptor();

	boolean isFetched();

	@Internal
	void clearFetched();

	@Override
	SqmPredicate getJoinPredicate();

	void setJoinPredicate(SqmPredicate predicate);

	@Override
	<S extends T> SqmAttributeJoin<O, S> treatAs(Class<S> treatJavaType);

	@Override
	<S extends T> SqmAttributeJoin<O, S> treatAs(EntityDomainType<S> treatTarget);

	@Override
	<S extends T> SqmAttributeJoin<O, S> treatAs(Class<S> treatJavaType, String alias);

	@Override
	<S extends T> SqmAttributeJoin<O, S> treatAs(EntityDomainType<S> treatJavaType, String alias);

	<S extends T> SqmAttributeJoin<O, S> treatAs(Class<S> treatJavaType, String alias, boolean fetch);

	<S extends T> SqmAttributeJoin<O, S> treatAs(EntityDomainType<S> treatJavaType, String alias, boolean fetch);

	/*
		@deprecated not used anymore
	 */
	@Deprecated
	@Remove
	SqmAttributeJoin makeCopy( SqmCreationProcessingState creationProcessingState );
}
