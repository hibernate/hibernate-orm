/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.context.spi;

/**
 * A callback registered with the {@link org.hibernate.SessionFactory} that is responsible for resolving the
 * current tenant identifier for use with {@link CurrentSessionContext} and
 * {@link org.hibernate.SessionFactory#getCurrentSession()}
 *
 * @author Steve Ebersole
 */
public interface CurrentTenantIdentifierResolver {
	/**
	 * Resolve the current tenant identifier.
	 * 
	 * @return The current tenant identifier
	 */
	public String resolveCurrentTenantIdentifier();

	/**
	 * Should we validate that the tenant identifier on "current sessions" that already exist when
	 * {@link CurrentSessionContext#currentSession()} is called matches the value returned here from
	 * {@link #resolveCurrentTenantIdentifier()}?
	 * 
	 * @return {@code true} indicates that the extra validation will be performed; {@code false} indicates it will not.
	 *
	 * @see org.hibernate.context.TenantIdentifierMismatchException
	 */
	public boolean validateExistingCurrentSessions();
}
