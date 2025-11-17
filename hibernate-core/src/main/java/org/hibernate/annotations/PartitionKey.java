/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Identifies a field of an entity that holds the
 * partition key of a table mapped by the entity
 * class.
 * <p>
 * If the partition key forms part of the unique
 * {@linkplain jakarta.persistence.Id identifier}
 * of the entity, this annotation is optional but
 * may still be applied for documentation purposes.
 * <p>
 * On the other hand, if the partition key is not
 * part of the identifier, use of this annotation
 * may improve the performance of SQL {@code update}
 * and {@code delete} statements.
 * <p>
 * <pre>
 * &#064;Entity
 * &#064;Table(name  = "partitioned_table",
 *     options =
 *         """
 *         partition by range (pid) (
 *             partition p1 values less than (1000),
 *             partition p2 values less than (2000)
 *         )
 *         """)
 * class Partitioned {
 *     &#064;Id &#064;GeneratedValue Long id;
 *     &#064;PartitionKey Long pid;
 *     String text;
 * }
 * </pre>
 * Many databases are not able to maintain a unique
 * key constraint across multiple partitions unless
 * the unique key contains the partition key column.
 * On these databases, the column mapped by a field
 * annotated {@code @PartitionKey} is automatically
 * added to the generated primary key constraint.
 * In this case, the database is not able to enforce
 * uniqueness of the identifier value, and care must
 * be taken to ensure that the identifier is unique
 * across entity instances.
 *
 * @since 6.2
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface PartitionKey {
}
