/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public interface PluralAttributeElementSource {
	PluralAttributeElementNature getNature();
}
