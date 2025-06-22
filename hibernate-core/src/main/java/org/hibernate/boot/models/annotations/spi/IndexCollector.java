/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.spi;

import java.lang.annotation.Annotation;

import jakarta.persistence.Index;

/**
 * Commonality for annotations which define indexes
 *
 * @author Steve Ebersole
 */
public interface IndexCollector extends Annotation {
	Index[] indexes();

	void indexes(Index[] value);
}
