/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * @author Steve Ebersole
 */
public interface FetchCharacteristicsPluralAttribute extends FetchCharacteristics {
	Integer getBatchSize();
	boolean isExtraLazy();
}
