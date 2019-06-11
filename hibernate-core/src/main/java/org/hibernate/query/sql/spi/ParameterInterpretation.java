/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryParameterImplementor;

/**
 * @author Steve Ebersole
 */
public interface ParameterInterpretation {
	/**
	 * Access to the defined parameters in the order they were encountered,
	 * potentially including "duplicate references" to a single parameter
	 */
	List<QueryParameterImplementor<?>> getOccurrenceOrderedParameters();

	/**
	 * Create the ParameterMetadata representation of this interpretation
	 */
	ParameterMetadataImplementor toParameterMetadata(SharedSessionContractImplementor session);
}
