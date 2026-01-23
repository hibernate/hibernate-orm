/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.transform;


import org.hibernate.query.TypedTupleTransformer;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

/**
 * @deprecated Not sure how useful these are anymore.
 */
@Deprecated
final public class Transformers {
	/**
	 * Creates a ResultTransformer that will inject aliased values into
	 * instances of Class via property methods or fields.
	 */
	public static <T> TypedTupleTransformer<T> beanTransformer(Class<T> target) {
		return AliasToBeanResultTransformer.forBeanClass(target);
	}

	/**
	 * Creates a ResultTransformer that will inject aliased values into
	 * instances of Class via property methods or fields.
	 */
	public static <T> TypedTupleTransformer<T> constructorTransformer(Constructor<T> constructor) {
		return AliasToBeanConstructorResultTransformer.forConstructor(constructor);
	}

	/**
	 * Essentially the same as using HQL {@code select new map(...) ...}.
	 * Ultimately transforms the result from {@code List<Object[]>} to {@code List<Map<String,Object>>}.
	 *
	 * @see jakarta.persistence.Tuple
	 */
	public static TypedTupleTransformer<Map<String,Object>> mapTransformer() {
		return AliasToEntityMapResultTransformer.INSTANCE;
	}

	/**
	 * Essentially the same as using HQL {@code select new list(...) ...}.
	 * Ultimately transforms the result from {@code List<Object[]>} to {@code List<List<Object>>}.
	 */
	public static TypedTupleTransformer<List<Object>> listTransformer() {
		return ToListResultTransformer.INSTANCE;
	}

	private Transformers() {}


	/**
	 * Creates a ResultTransformer that will inject aliased values into
	 * instances of Class via property methods or fields.
	 */
	public static <T> TypedTupleTransformer<T> aliasToBean(Class<T> target) {
		return beanTransformer( target );
	}

	/**
	 * Creates a ResultTransformer that will inject aliased values into
	 * instances of Class via property methods or fields.
	 */
	public static <T> TypedTupleTransformer<T> aliasToConstructor(Constructor<T> constructor) {
		return constructorTransformer(constructor);
	}

	/**
	 * Each row of results is a {@code Map} from alias to values/entities
	 */
	public static final AliasToEntityMapResultTransformer ALIAS_TO_ENTITY_MAP =
			AliasToEntityMapResultTransformer.INSTANCE;

	/**
	 * Each row of results is a {@code List}
	 */
	public static final ToListResultTransformer TO_LIST = ToListResultTransformer.INSTANCE;
}
