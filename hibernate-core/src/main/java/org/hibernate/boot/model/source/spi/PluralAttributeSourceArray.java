/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.Remove;

import org.hibernate.boot.model.source.internal.hbm.IndexedPluralAttributeSource;

/**
 * @author Steve Ebersole
 */
@Remove
public interface PluralAttributeSourceArray extends IndexedPluralAttributeSource {
	String getElementClass();
}
