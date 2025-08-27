/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id;

import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.id.enhanced.StandardOptimizerDescriptor;

/**
 * Commonality between sequence-based and table-based generators
 */
public interface OptimizableGenerator extends IdentifierGenerator, ExportableProducer {
	/**
	 * If an explicit sequence/table name is not configured,
	 */
	String IMPLICIT_NAME_BASE = "implicit_name_base";

	/**
	 * Indicates the initial value to use.  The default value is {@link #DEFAULT_INITIAL_VALUE}
	 */
	String INITIAL_PARAM = "initial_value";

	/**
	 * The default value for {@link #INITIAL_PARAM}
	 */
	int DEFAULT_INITIAL_VALUE = 1;

	/**
	 * Indicates the increment size to use.  The default value is {@link #DEFAULT_INCREMENT_SIZE}
	 */
	String INCREMENT_PARAM = "increment_size";

	/**
	 * The default value for {@link #INCREMENT_PARAM}
	 */
	int DEFAULT_INCREMENT_SIZE = 50;

	/**
	 * Indicates the optimizer to use, either naming a {@link Optimizer} implementation class or naming
	 * a {@link StandardOptimizerDescriptor} by name.  Takes precedence over {@link #INCREMENT_PARAM}.
	 */
	String OPT_PARAM = "optimizer";

	Optimizer getOptimizer();
}
