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

import org.hibernate.testing.jdbc.SQLStatementInspector;

/**
 * @author Steve Ebersole
 */
public interface SessionFactoryScope {
	SessionFactoryImplementor getSessionFactory();
	MetadataImplementor getMetadataImplementor();
	StatementInspector getStatementInspector();
	<T extends StatementInspector> T getStatementInspector(Class<T> type);
	SQLStatementInspector getCollectingStatementInspector();

	default void withSessionFactory(Consumer<SessionFactoryImplementor> action) {
		action.accept( getSessionFactory() );
	}

	void inSession(Consumer<SessionImplementor> action);
	void inTransaction(Consumer<SessionImplementor> action);
	void inTransaction(SessionImplementor session, Consumer<SessionImplementor> action);

	<T> T fromSession(Function<SessionImplementor, T> action);
	<T> T fromTransaction(Function<SessionImplementor, T> action);
	<T> T fromTransaction(SessionImplementor session, Function<SessionImplementor, T> action);

	void inStatelessSession(Consumer<StatelessSessionImplementor> action);
	void inStatelessTransaction(Consumer<StatelessSessionImplementor> action);
	void inStatelessTransaction(StatelessSessionImplementor session, Consumer<StatelessSessionImplementor> action);

	void dropData();
}
