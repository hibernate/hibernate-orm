/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.resource.transaction;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

/**
 * A builder of TransactionCoordinator instances intended for use in JTA environments.
 *
 * @author Steve Ebersole
 */
public interface TransactionCoordinatorJtaBuilder extends TransactionCoordinatorBuilder {
	/**
	 * Specifies the JtaPlatform to use.
	 *
	 * @param jtaPlatform The JtaPlatform to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	public TransactionCoordinatorJtaBuilder setJtaPlatform(JtaPlatform jtaPlatform);

	/**
	 * Should JTA transactions be automatically joined?  Or should we wait for (JPA-style) explicit joining?  The
	 * default is to auto-join ({@code true}).
	 *
	 * @param autoJoinTransactions {@code true} (default) indicates that JTA transactions should be auto joined;
	 * {@code false} indicated we should wait for an explicit join
	 *
	 * @return {@code this}, for method chaining
	 */
	public TransactionCoordinatorJtaBuilder setAutoJoinTransactions(boolean autoJoinTransactions);

	/**
	 * Should we prefer to use UserTransactions (over TransactionManager) for managing transactions (mainly for calling
	 * begin, commit, rollback)?  We will try both, this controls which to check first.  The default is to prefer
	 * accessing the TransactionManager
	 *
	 * @param preferUserTransactions {@code true} indicates to look for UserTransactions first; {@code false} (the
	 * default) indicates to looks for the TransactionManager first,
	 *
	 * @return {@code this}, for method chaining
	 */
	public TransactionCoordinatorJtaBuilder setPreferUserTransactions(boolean preferUserTransactions);

	/**
	 * Should we track threads in order to protect against the JTA system calling us from a different thread?  This
	 * might often be the case for JTA systems which implement timeout rollbacks from separate "reaper" threads.  The
	 * default is to track threads.
	 *
	 * @param performJtaThreadTracking {@code true} (the default) indicates that the thread should be tracked;
	 * {@code false} indicates it should not.
	 *
	 * @return {@code this}, for method chaining
	 */
	public TransactionCoordinatorJtaBuilder setPerformJtaThreadTracking(boolean performJtaThreadTracking);
}
