/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;

/**
 * @author Steve Ebersole
 */
public interface FetchCharacteristics {
	FetchTiming getFetchTiming();
	FetchStyle getFetchStyle();
}
