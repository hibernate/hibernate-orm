/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import org.hibernate.LockOptions;

/**
 * Encapsulation of the options for performing a load by multiple identifiers.
 *
 * @author Steve Ebersole
 */
public interface MultiLoadOptions {
	boolean isSessionCheckingEnabled();
	boolean isReturnOfDeletedEntitiesEnabled();
	boolean isOrderReturnEnabled();

	LockOptions getLockOptions();

	Integer getBatchSize();
}
