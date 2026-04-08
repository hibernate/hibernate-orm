/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import org.hibernate.testing.jdbc.CollectingStatementObserver;
import org.hibernate.testing.jdbc.SQLStatementInspector;

/**
 * @author Steve Ebersole
 */
public interface SessionFactoryScope {
	SessionFactoryImplementor getSessionFactory();
	MetadataImplementor getMetadataImplementor();

	/**
	 * Access to the CollectingStatementObserver for this test, if one.
	 *
	 * @see SessionFactory#useCollectingStatementObserver()
	 */
	CollectingStatementObserver getCollectingStatementObserver();

	/**
	 * @deprecated Along with {@linkplain SessionFactory#useCollectingStatementInspector()};
	 * use {@linkplain #getCollectingStatementObserver()} instead.
	 */
	@Deprecated(forRemoval = true, since = "8.0")
	StatementInspector getStatementInspector();

	/**
	 * @deprecated Along with {@linkplain SessionFactory#useCollectingStatementInspector()};
	 * use {@linkplain #getCollectingStatementObserver()} instead.
	 */
	@Deprecated(forRemoval = true, since = "8.0")
	<T extends StatementInspector> T getStatementInspector(Class<T> type);

	/**
	 * @deprecated Along with {@linkplain SessionFactory#useCollectingStatementInspector()};
	 * use {@linkplain #getCollectingStatementObserver()} instead.
	 */
	@Deprecated(forRemoval = true, since = "8.0")
	SQLStatementInspector getCollectingStatementInspector();

	void releaseSessionFactory();

	default void withSessionFactory(Consumer<SessionFactoryImplementor> action) {
		action.accept( getSessionFactory() );
	}

	void inSession(Consumer<SessionImplementor> action);
	void inTransaction(Consumer<SessionImplementor> action);
	void inTransaction(SessionImplementor session, Consumer<SessionImplementor> action);
	void inTransaction(Function<SessionFactoryImplementor,SessionImplementor> sessionProducer, Consumer<SessionImplementor> action);

	<T> T fromSession(Function<SessionImplementor, T> action);
	<T> T fromTransaction(Function<SessionImplementor, T> action);
	<T> T fromTransaction(SessionImplementor session, Function<SessionImplementor, T> action);
	<T> T fromTransaction(Function<SessionFactoryImplementor,SessionImplementor> sessionProducer, Function<SessionImplementor, T> action);

	void inStatelessSession(Consumer<StatelessSessionImplementor> action);
	void inStatelessTransaction(Consumer<StatelessSessionImplementor> action);
	void inStatelessTransaction(StatelessSessionImplementor session, Consumer<StatelessSessionImplementor> action);

	void dropData();
}
