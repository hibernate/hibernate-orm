/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import org.hibernate.Incubating;

import jakarta.persistence.Tuple;

/**
 * Hibernate extension to the Jakarta Persistence {@link Tuple}
 * contract
 *
 * @author Steve Ebersole
 */
@Incubating
public interface JpaTuple extends Tuple {
}
