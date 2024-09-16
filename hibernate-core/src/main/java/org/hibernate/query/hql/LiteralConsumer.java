/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.hql;

/**
 * Pluggable contract for consuming literals encountered in an HQL query.
 *
 * todo (6.0) : hook this in.  proposed syntax - `{literal [name] [value]}`
 * 		- where [name] is a registration key identifying the consumer to use
 * 			and [value] is the literal value to be consumed.  Potentially allow
 * 			multiple tokens after [name] to allow for "hints" to the consumer.
 *
 * todo (6.0) : use JDK ServiceLoader + explicitly specified
 *
 * @author Steve Ebersole
 */
public interface LiteralConsumer {
}
