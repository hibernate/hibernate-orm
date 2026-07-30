/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.Remove;

/**
 * @author Steve Ebersole
 */
@Remove
public interface FetchCharacteristicsPluralAttribute extends FetchCharacteristics {
	Integer getBatchSize();
	boolean isExtraLazy();
}
