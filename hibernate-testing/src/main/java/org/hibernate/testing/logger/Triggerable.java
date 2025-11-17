/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.logger;

import java.util.Collections;
import java.util.List;

public interface Triggerable {

	String triggerMessage();

	boolean wasTriggered();

	void reset();

	default List<String> triggerMessages() {
		return Collections.singletonList( triggerMessage() );
	}
}
