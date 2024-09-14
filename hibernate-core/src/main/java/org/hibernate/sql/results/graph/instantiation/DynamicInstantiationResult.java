/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.results.graph.instantiation;

import org.hibernate.sql.results.graph.DomainResult;

/**
 * Specialization of DomainResult to model
 * {@linkplain jakarta.persistence.ConstructorResult dynamic instantiation}
 *
 * @author Steve Ebersole
 */
public interface DynamicInstantiationResult<R> extends DomainResult<R> {
}
