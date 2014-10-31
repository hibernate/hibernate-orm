/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.event.internal;

import org.hibernate.event.spi.EntityCopyObserver;
import org.hibernate.event.spi.EventSource;

/**
 * An {@link org.hibernate.event.spi.EntityCopyObserver} implementation that allows multiple representations of
 * the same persistent entity to be merged.
 *
 * @author Gail Badner
 */
public class EntityCopyAllowedObserver implements EntityCopyObserver {

	public static final String SHORT_NAME = "allow";

	@Override
	public void entityCopyDetected(
			Object managedEntity,
			Object mergeEntity1,
			Object mergeEntity2,
			EventSource session) {
		// do nothing.
	}

	public void clear() {
		// do nothing.
	}


	@Override
	public void topLevelMergeComplete(EventSource session) {
		// do nothing.
	}
}
