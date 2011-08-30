/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.flush;

import java.util.Collection;

import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;

/**
 * @author Steve Ebersole
 */
public class InitializingPreUpdateEventListener implements PreUpdateEventListener {
	@Override
	public boolean onPreUpdate(PreUpdateEvent event) {
        final Object entity = event.getEntity();
        final Object[] oldValues = event.getOldState();
        final String[] properties = event.getPersister().getPropertyNames();

        // Iterate through all fields of the updated object
        for ( int i = 0; i < properties.length; i++ ) {
            if (oldValues != null && oldValues[i] instanceof Collection ) {
                final Collection col = (Collection) oldValues[i];
				// force initialization of collection to illustrate HHH-2763
				for ( Object element : col ) {
					element.toString();
                }
            }
        }
        return true;
	}
}
