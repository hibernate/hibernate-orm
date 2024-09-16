/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.spi;

import java.lang.annotation.Annotation;

/**
 * @author Steve Ebersole
 */
public interface RepeatableContainer<R extends Annotation> extends Annotation {
	R[] value();

	void value(R[] value);
}
