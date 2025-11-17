/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.service.spi;

import org.hibernate.Incubating;

@Incubating
@FunctionalInterface
public interface EventActionWithParameter<T, U, X> {

	void applyEventToListener(T eventListener, U action, X param);

}
