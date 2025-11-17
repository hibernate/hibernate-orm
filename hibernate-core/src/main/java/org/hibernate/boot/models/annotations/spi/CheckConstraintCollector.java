/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.spi;

import java.lang.annotation.Annotation;

import jakarta.persistence.CheckConstraint;

/**
 * Commonality for annotations which define check-constraints
 *
 * @author Steve Ebersole
 */
public interface CheckConstraintCollector extends Annotation {
	CheckConstraint[] check();

	void check(CheckConstraint[] value);
}
