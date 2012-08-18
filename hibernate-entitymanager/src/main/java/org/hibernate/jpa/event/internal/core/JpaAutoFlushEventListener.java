/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.event.internal.core;

import java.util.IdentityHashMap;

import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.event.internal.DefaultAutoFlushEventListener;
import org.hibernate.event.spi.AutoFlushEventListener;

/**
 * In JPA, it is the create operation that is cascaded to unmanaged entities at flush time (instead of the save-update
 * operation in Hibernate).
 *
 * @author Gavin King
 */
public class JpaAutoFlushEventListener
		extends DefaultAutoFlushEventListener
		implements HibernateEntityManagerEventListener {

	public static final AutoFlushEventListener INSTANCE = new JpaAutoFlushEventListener();

	@Override
	protected CascadingAction getCascadingAction() {
		return CascadingActions.PERSIST_ON_FLUSH;
	}

	@Override
	protected Object getAnything() {
		return new IdentityHashMap( 10 );
	}

}
