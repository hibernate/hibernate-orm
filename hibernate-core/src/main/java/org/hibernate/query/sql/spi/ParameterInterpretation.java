/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.spi;

import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.ParameterMetadataImplementor;

/**
 * @author Steve Ebersole
 */
public interface ParameterInterpretation {
	/**
	 * Access to the defined parameters in the order they were encountered,
	 * potentially including "duplicate references" to a single parameter
	 */
	List<ParameterOccurrence> getOrderedParameterOccurrences();

	/**
	 * Create the ParameterMetadata representation of this interpretation
	 */
	ParameterMetadataImplementor toParameterMetadata(SharedSessionContractImplementor session);

	String getAdjustedSqlString();
}
