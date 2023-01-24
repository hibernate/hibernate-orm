/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the tables that hold state mapped by the annotated entity.
 * <p>
 * If Hibernate is not aware that a certain table holds state mapped
 * by an entity class, then {@linkplain org.hibernate.FlushMode#AUTO
 * auto-flush} might not occur when it should, and queries against the
 * entity might return stale data.
 * <p>
 * This annotation might be necessary if:
 * <ul>
 * <li>the entity maps a database view,
 * <li>the entity is persisted using handwritten SQL, that is, using
 *     {@link SQLSelect @SQLSelect} and friends, or
 * <li>the entity is mapped using {@link Subselect @Subselect}.
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
 */
@Target(TYPE)
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
