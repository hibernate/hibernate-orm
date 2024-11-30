/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

/**
 * Defines an event class for the flushing of a session.
 *
 * @author Steve Ebersole
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
