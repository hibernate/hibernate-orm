/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.compile;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.spi.QueryImplementor;

/**
 * The interpretation of a JPA criteria object.
 *
 * @author Steve Ebersole
 */
public interface CriteriaInterpretation {
	/**
	 * Generate a {@link javax.persistence.Query} instance given the interpreted criteria compiled against the
	 * passed EntityManager.
	 *
	 *
	 * @param entityManager The EntityManager against which to create the Query instance.
	 * @param interpretedParameterMetadata parameter metadata
	 *
	 * @return The created Query instance.
	 */
	QueryImplementor buildCompiledQuery(SessionImplementor entityManager, InterpretedParameterMetadata interpretedParameterMetadata);
}
