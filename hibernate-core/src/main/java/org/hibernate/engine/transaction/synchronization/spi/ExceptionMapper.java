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
package org.hibernate.engine.transaction.synchronization.spi;

import java.io.Serializable;
import javax.transaction.SystemException;

/**
 * A pluggable strategy for defining how the {@link javax.transaction.Synchronization} registered by Hibernate handles
 * exceptions.
 *
 * @author Steve Ebersole
 */
public interface ExceptionMapper extends Serializable {
	/**
	 * Map a JTA {@link javax.transaction.SystemException} to the appropriate runtime-based exception.
	 *
	 * @param message The message to use for the returned exception
	 * @param systemException The causal exception
	 *
	 * @return The appropriate exception to throw
	 */
	public RuntimeException mapStatusCheckFailure(String message, SystemException systemException);

	/**
	 * Map an exception encountered during a managed flush to the appropriate runtime-based exception.
	 *
	 * @param message The message to use for the returned exception
	 * @param failure The causal exception
	 *
	 * @return The appropriate exception to throw
	 */
	public RuntimeException mapManagedFlushFailure(String message, RuntimeException failure);
}
