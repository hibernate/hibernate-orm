/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.service.spi;

import org.hibernate.Incubating;

@Incubating
@FunctionalInterface
public interface EventActionWithParameter<T, U, X> {

	void applyEventToListener(T eventListener, U action, X param);

}
