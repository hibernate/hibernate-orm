/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

/**
 * Event class for stateful session flush.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.Session#flush
 */
public class FlushEvent extends AbstractEvent {
	private int numberOfEntitiesProcessed;
	private int numberOfCollectionsProcessed;

	public FlushEvent(EventSource source) {
		super( source );
	}

	public int getNumberOfEntitiesProcessed() {
		return numberOfEntitiesProcessed;
	}

	public void setNumberOfEntitiesProcessed(int numberOfEntitiesProcessed) {
		this.numberOfEntitiesProcessed = numberOfEntitiesProcessed;
	}

	public int getNumberOfCollectionsProcessed() {
		return numberOfCollectionsProcessed;
	}

	public void setNumberOfCollectionsProcessed(int numberOfCollectionsProcessed) {
		this.numberOfCollectionsProcessed = numberOfCollectionsProcessed;
	}
}
