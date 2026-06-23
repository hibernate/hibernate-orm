/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.service.spi;

import org.hibernate.Incubating;
import jakarta.annotation.Nonnull;

@Incubating
@FunctionalInterface
public interface EventActionWithParameter<T, U, X> {

	void applyEventToListener(@Nonnull T eventListener, @Nonnull U action, @Nonnull X param);

}
