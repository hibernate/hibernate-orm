/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies a table or tables that hold state mapped by the annotated
 * entity or collection.
 * <p>
 * If Hibernate is not aware that a certain table holds state mapped
 * by an entity class or collection, then modifications might not be
 * {@linkplain org.hibernate.FlushMode#AUTO automatically synchronized}
 * with the database before a query is executed against that table, and
 * the query might return stale data.
 * <p>
 * Ordinarily, Hibernate knows the tables containing the state of an
 * entity or collection. This annotation might be necessary if:
 * <ul>
 * <li>an entity or collection maps a database {@linkplain View view},
 * <li>an entity or collection is persisted using handwritten SQL,
 *     that is, using {@link SQLSelect @SQLSelect} and friends, or
 * <li>an entity is mapped using {@link Subselect @Subselect}.
 * </ul>
 * <p>
 * By default, the table names specified by this annotation are interpreted
 * as {@linkplain org.hibernate.boot.model.naming.PhysicalNamingStrategy
 * logical names}, and are processed by
 * {@link org.hibernate.boot.model.naming.PhysicalNamingStrategy#toPhysicalTableName}.
 * But if {@link #logical logical=false}, the table names will be treated
 * as physical names, and will not be processed by the naming strategy.
 *
 * @author Sharath Reddy
 *
 * @see org.hibernate.query.SynchronizeableQuery
 * @see View
 * @see Subselect
 * @see SQLSelect
 */
@Target({TYPE, FIELD, METHOD})
@Retention(RUNTIME)
public @interface Synchronize {
	/**
	 * Names of tables that hold state mapped by the annotated entity.
	 * Updates to these tables must be flushed to the database before
	 * execution of any query which refers to the annotated entity.
	 */
	String[] value();

	/**
	 * Specifies whether the table names given by {@link #value}
	 * should be interpreted as logical or physical names.
	 *
	 * @return {@code true} if they are logical names
	 */
	boolean logical() default true;
}
