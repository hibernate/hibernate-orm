/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.EntityDomainType;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Predicate;

/**
 * Specialization of {@link JpaJoin} for {@link java.util.Map} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface JpaMapJoin<O,K,V> extends JpaPluralJoin<O, Map<K, V>, V>, MapJoin<O,K,V> {
	@Override
	JpaMapJoin<O, K, V> on(@Nullable JpaExpression<Boolean> restriction);

	@Override
	JpaMapJoin<O, K, V> on(@Nullable Expression<Boolean> restriction);

	@Override
	JpaMapJoin<O, K, V> on(JpaPredicate @Nullable... restrictions);

	@Override
	JpaMapJoin<O, K, V> on(Predicate @Nullable... restrictions);

	@Override
	<S extends V> JpaTreatedJoin<O, V, S> treatAs(Class<S> treatAsType);

	@Override
	<S extends V> JpaTreatedJoin<O, V, S> treatAs(EntityDomainType<S> treatJavaType);
}
