/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;
import jakarta.annotation.Nonnull;



/**
 * Event class for {@link org.hibernate.Session#isDirty}.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.Session#isDirty
 */
public class DirtyCheckEvent extends AbstractSessionEvent {
	private boolean dirty;

	public DirtyCheckEvent(@Nonnull EventSource source) {
		super(source);
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

}
