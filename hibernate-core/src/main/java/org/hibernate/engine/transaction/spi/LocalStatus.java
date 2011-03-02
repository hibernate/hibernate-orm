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
package org.hibernate.engine.transaction.spi;

/**
 * Enumeration of statuses in which a local transaction facade ({@link org.hibernate.Transaction}) might be.
 *
 * @author Steve Ebersole
 */
public enum LocalStatus {
	/**
	 * The local transaction has not yet been begun
	 */
	NOT_ACTIVE,
	/**
	 * The local transaction has been begun, but not yet completed.
	 */
	ACTIVE,
	/**
	 * The local transaction has been competed successfully.
	 */
	COMMITTED,
	/**
	 * The local transaction has been rolled back.
	 */
	ROLLED_BACK,
	/**
	 * The local transaction attempted to commit, but failed.
	 */
	FAILED_COMMIT
}
