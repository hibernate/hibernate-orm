/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.selectenumproperty;

public enum TopicStatus {
	TOPIC_UNLOCKED( 0 ),
	TOPIC_LOCKED( 1 );

	private final int topicStatus;

	TopicStatus(final int topicStatus) {
		this.topicStatus = topicStatus;
	}

	public int topicStatus() {
		return topicStatus;
	}
}
