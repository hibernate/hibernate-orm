/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal;

import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.engine.creation.internal.options.StatelessOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

/**
 * @author Steve Ebersole
 */
public abstract class StatelessSessionBuilderImpl
		extends AbstractCommonBuilder<StatelessSessionBuilder>
		implements StatelessSessionBuilder {
	private final StatelessOptions options;

	public StatelessSessionBuilderImpl(SessionFactoryImplementor sessionFactory) {
		this( sessionFactory, new StatelessOptions( sessionFactory ) );
	}

	protected StatelessSessionBuilderImpl(SessionFactoryImplementor sessionFactory, StatelessOptions options) {
		super( sessionFactory, options );
		this.options = options;
	}

	@Override
	protected StatelessSessionBuilder getThis() {
		return this;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// StatelessSessionBuilder

	@Override
	public StatelessSession open() {
		return openStatelessSession();
	}

	@Override
	public StatelessSession openStatelessSession() {
		CORE_LOGGER.openingStatelessSession( options.getTenantIdentifierValue() );
		return createStatelessSession( options );
	}

	protected abstract StatelessSessionImplementor createStatelessSession(StatelessOptions options);

	@Override
	@Deprecated
	public StatelessSessionBuilder statementInspector(StatementInspector statementInspector) {
		options.statementInspector( statementInspector );
		return this;
	}
}
