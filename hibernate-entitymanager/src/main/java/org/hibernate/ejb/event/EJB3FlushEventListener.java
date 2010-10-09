/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.ejb.event;

import org.hibernate.engine.CascadingAction;
import org.hibernate.event.FlushEventListener;
import org.hibernate.event.def.DefaultFlushEventListener;
import org.hibernate.util.IdentityMap;

/**
 * In EJB3, it is the create operation that is cascaded to unmanaged
 * ebtities at flush time (instead of the save-update operation in
 * Hibernate).
 *
 * @author Gavin King
 */
public class EJB3FlushEventListener extends DefaultFlushEventListener {

	public static final FlushEventListener INSTANCE = new EJB3FlushEventListener();

	protected CascadingAction getCascadingAction() {
		return CascadingAction.PERSIST_ON_FLUSH;
	}

	protected Object getAnything() {
		return IdentityMap.instantiate( 10 );
	}

}
