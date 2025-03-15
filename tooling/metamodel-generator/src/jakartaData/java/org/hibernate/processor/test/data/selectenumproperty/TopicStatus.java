/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
