/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception.spi;

/**
 * @author Steve Ebersole
 */
public interface ConversionContext {
	ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor();
}
