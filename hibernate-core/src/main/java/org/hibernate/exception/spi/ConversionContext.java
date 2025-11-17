/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception.spi;

/**
 * @author Steve Ebersole
 */
public interface ConversionContext {
	ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor();
}
