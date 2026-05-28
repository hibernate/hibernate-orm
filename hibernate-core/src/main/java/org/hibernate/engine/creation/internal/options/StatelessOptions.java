/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal.options;

import jakarta.persistence.EntityAgent;
import org.hibernate.FlushMode;
import org.hibernate.SessionEventListener;
import org.hibernate.engine.creation.internal.SessionCreationOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.OptionsHelper;

import java.util.List;

/// Mutable collector for options which apply to stateless
/// {@linkplain org.hibernate.StatelessSession sessions}.
///
/// Stateless sessions share the common creation options but do not support the
/// stateful-only knobs represented by [StatefulOptions]. The inherited
/// [SessionCreationOptions] methods for those stateful-only settings
/// return the fixed values expected by stateless session construction.
///
/// @since 8.0
/// @author Steve Ebersole
public class StatelessOptions extends CommonOptions implements SessionCreationOptions {
	public StatelessOptions(SessionFactoryImplementor sessionFactory) {
		super( sessionFactory );
	}

	/**
	 * Apply a Jakarta Persistence creation option to this collector.
	 */
	public StatelessOptions apply(EntityAgent.CreationOption option) {
		OptionsHelper.applyOption( this, option );
		return this;
	}

	/**
	 * Apply Jakarta Persistence creation options to this collector.
	 */
	public StatelessOptions apply(EntityAgent.CreationOption... options) {
		if ( options != null ) {
			for ( var option : options ) {
				apply( option );
			}
		}
		return this;
	}

	@Override
	public boolean shouldAutoJoinTransactions() {
		return true;
	}

	@Override
	public FlushMode getInitialSessionFlushMode() {
		return FlushMode.ALWAYS;
	}

	@Override
	public boolean isSubselectFetchEnabled() {
		return false;
	}

	@Override
	public int getDefaultBatchFetchSize() {
		return -1;
	}

	@Override
	public boolean shouldAutoClose() {
		return false;
	}

	@Override
	public boolean shouldAutoClear() {
		return false;
	}

	@Override
	public boolean isIdentifierRollbackEnabled() {
		// identifier rollback is not yet implemented for StatelessSessions
		return false;
	}

	@Override
	public List<SessionEventListener> getCustomSessionEventListeners() {
		return null;
	}
}
