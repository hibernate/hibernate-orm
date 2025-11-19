/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import java.util.List;

/// Allows watching of log messages.
///
/// @see LoggingInspections
/// @see MessageKeyInspection
///
/// @author Steve Ebersole
public interface MessageKeyWatcher {
	String getMessageKey();

	boolean wasTriggered();

	List<String> getTriggeredMessages();

	String getFirstTriggeredMessage();

	void reset();
}
