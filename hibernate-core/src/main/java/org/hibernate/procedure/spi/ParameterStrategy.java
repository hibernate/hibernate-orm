/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.spi;

/**
 * The style/strategy of parameter registration used in a particular procedure call definition.
 */
public enum ParameterStrategy {
	/**
	 * The parameters are named
	 */
	NAMED,
	/**
	 * The parameters are positional
	 */
	POSITIONAL,
	/**
	 * We do not (yet) know
	 */
	UNKNOWN
}
