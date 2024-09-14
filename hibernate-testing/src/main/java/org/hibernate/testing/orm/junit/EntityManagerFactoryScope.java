/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.orm.junit;

import java.util.function.Consumer;
import java.util.function.Function;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import org.hibernate.testing.jdbc.SQLStatementInspector;

/**
 * @author Steve Ebersole
 */
public interface EntityManagerFactoryScope {
	EntityManagerFactory getEntityManagerFactory();
	void releaseEntityManagerFactory();

	StatementInspector getStatementInspector();
	<T extends StatementInspector> T getStatementInspector(Class<T> type);
	SQLStatementInspector getCollectingStatementInspector();

	void inEntityManager(Consumer<EntityManager> action);
	void inTransaction(Consumer<EntityManager> action);
	void inTransaction(EntityManager entityManager, Consumer<EntityManager> action);

	<T> T fromEntityManager(Function<EntityManager, T> action);
	<T> T fromTransaction(Function<EntityManager, T> action);
	<T> T fromTransaction(EntityManager entityManager, Function<EntityManager, T> action);

	default Dialect getDialect() {
		return ((SessionFactoryImplementor) getEntityManagerFactory()).getJdbcServices().getDialect();
	}

}
