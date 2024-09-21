/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface MessageKeyWatcher {
	String getMessageKey();

	boolean wasTriggered();

	List<String> getTriggeredMessages();

	String getFirstTriggeredMessage();

	void reset();
}
