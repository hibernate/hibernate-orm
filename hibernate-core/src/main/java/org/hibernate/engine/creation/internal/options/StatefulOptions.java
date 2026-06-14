/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal.options;

import jakarta.persistence.EntityManager;
import org.hibernate.FlushMode;
import org.hibernate.SessionEventListener;
import org.hibernate.engine.creation.internal.SessionCreationOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.OptionsHelper;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.addAll;

/// Mutable collector for options which only apply to
/// [stateful sessions][org.hibernate.Session].
///
/// This extends [CommonOptions] with stateful session concerns such as
/// flush mode, transaction auto-join, auto-close/auto-clear, identifier
/// rollback, and custom [listeners][SessionEventListener]. The same
/// object is later exposed as [SessionCreationOptions] to the stateful
/// session constructor.
///
/// @since 8.0
/// @author Steve Ebersole
public class StatefulOptions extends CommonOptions implements SessionCreationOptions {
	private boolean autoJoinTransactions = true;
	private boolean autoClose;
	private boolean autoClear;
	private boolean identifierRollback;
	private FlushMode flushMode;

	// Lazy: defaults are built only when overriding the factory-level listeners.
	// Need a fresh build for each Session as the listener instances can't be
	// reused across sessions.
	private List<SessionEventListener> listeners;

	public StatefulOptions(SessionFactoryImplementor sessionFactory) {
		super( sessionFactory );
		final var options = sessionFactory.getSessionFactoryOptions();
		autoClose = options.isAutoCloseSessionEnabled();
		identifierRollback = options.isIdentifierRollbackEnabled();
	}

	/**
	 * Apply a Jakarta Persistence creation option to this collector.
	 */
	public StatefulOptions apply(EntityManager.CreationOption option) {
		OptionsHelper.applyOption( this, option );
		return this;
	}

	/**
	 * Apply Jakarta Persistence creation options to this collector.
	 */
	public StatefulOptions apply(EntityManager.CreationOption... options) {
		if ( options != null ) {
			for ( var option : options ) {
				apply( option );
			}
		}
		return this;
	}

	@Override
	public boolean shouldAutoJoinTransactions() {
		return autoJoinTransactions;
	}

	@Override
	public FlushMode getInitialSessionFlushMode() {
		return flushMode;
	}

	@Override
	public boolean shouldAutoClose() {
		return autoClose;
	}

	@Override
	public boolean shouldAutoClear() {
		return autoClear;
	}

	@Override
	public boolean isIdentifierRollbackEnabled() {
		return identifierRollback;
	}

	@Override
	public List<SessionEventListener> getCustomSessionEventListeners() {
		return listeners;
	}

	public void autoJoinTransactions(boolean autoJoinTransactions) {
		this.autoJoinTransactions = autoJoinTransactions;
	}

	public void autoClose(boolean autoClose) {
		this.autoClose = autoClose;
	}

	public void autoClear(boolean autoClear) {
		this.autoClear = autoClear;
	}

	public void flushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
	}

	public void identifierRollback(boolean identifierRollback) {
		this.identifierRollback = identifierRollback;
	}

	/// Append custom session event listeners, initializing the list from the
	/// factory defaults on first customization.
	///
	/// A fresh listener list is required for each session because listener
	/// instances are not reusable across sessions.
	public void eventListeners(SessionFactoryImplementor sessionFactory, SessionEventListener... listeners) {
		if ( this.listeners == null ) {
			final var baselineListeners =
					sessionFactory.getSessionFactoryOptions().buildSessionEventListeners();
			this.listeners = new ArrayList<>( baselineListeners.length + listeners.length );
			addAll( this.listeners, baselineListeners );
		}
		addAll( this.listeners, listeners );
	}

	/// Explicitly clear the event listener list.
	///
	/// A `null` listener list means "use factory defaults", so clearing must
	/// initialize an empty list.
	public void clearEventListeners() {
		if ( listeners == null ) {
			// Needs to initialize explicitly to an empty list as otherwise "null" implies the default listeners will be applied.
			listeners = new ArrayList<>( 3 );
		}
		else {
			listeners.clear();
		}
	}
}
