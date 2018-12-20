/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm;

import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * @author Steve Ebersole
 */
public interface SessionFactoryScope {
	SessionFactoryImplementor getSessionFactory();

	void inSession(Consumer<SessionImplementor> action);
	void inTransaction(Consumer<SessionImplementor> action);

	void inSession(SessionFactoryImplementor sfi, Consumer<SessionImplementor> action);
	void inTransaction(SessionFactoryImplementor factory, Consumer<SessionImplementor> action);

	void inTransaction(SessionImplementor session, Consumer<SessionImplementor> action);
}
