/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;
import jakarta.annotation.Nonnull;


/**
 * Event class for {@link org.hibernate.Session#clear}.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.Session#clear
 */
public class ClearEvent extends AbstractSessionEvent {
	public ClearEvent(@Nonnull EventSource source) {
		super( source );
	}
}
