/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
