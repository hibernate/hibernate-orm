/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.StatelessSession;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
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

	void inSession(Consumer<SessionImplementor> action);
	void inTransaction(Consumer<SessionImplementor> action);
	void inTransaction(SessionImplementor session, Consumer<SessionImplementor> action);

	<T> T fromSession(Function<SessionImplementor, T> action);
	<T> T fromTransaction(Function<SessionImplementor, T> action);
	<T> T fromTransaction(SessionImplementor session, Function<SessionImplementor, T> action);

	void inStatelessSession(Consumer<StatelessSession> action);
	void inStatelessTransaction(Consumer<StatelessSession> action);
	void inStatelessTransaction(StatelessSession session, Consumer<StatelessSession> action);
}
