/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.transform;


/**
 * @deprecated since {@link ResultTransformer} is deprecated
 */
@Deprecated
final public class Transformers {

	private Transformers() {}

	/**
	 * Each row of results is a {@code Map} from alias to values/entities
	 */
	public static final AliasToEntityMapResultTransformer ALIAS_TO_ENTITY_MAP =
			AliasToEntityMapResultTransformer.INSTANCE;

	/**
	 * Each row of results is a {@code List}
	 */
	public static final ToListResultTransformer TO_LIST = ToListResultTransformer.INSTANCE;

	/**
	 * Creates a ResultTransformer that will inject aliased values into
	 * instances of Class via property methods or fields.
	 */
	public static <T> ResultTransformer<T> aliasToBean(Class<T> target) {
		return new AliasToBeanResultTransformer<>(target);
	}

}
