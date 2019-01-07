/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * @author Steve Ebersole
 */
public interface SessionFactoryScope {
	SessionFactoryImplementor getSessionFactory();

	void inSession(Consumer<SessionImplementor> action);

	void inTransaction(Consumer<SessionImplementor> action);
	void inTransaction(SessionImplementor session, Consumer<SessionImplementor> action);

	<T> T fromSession(Function<SessionImplementor,T> action);

	<T> T fromTransaction(Function<SessionImplementor,T> action);
	<T> T fromTransaction(SessionImplementor session, Function<SessionImplementor,T> action);
}
