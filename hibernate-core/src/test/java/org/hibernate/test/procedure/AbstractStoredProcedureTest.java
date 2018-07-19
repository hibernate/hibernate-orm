/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.procedure;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.function.Consumer;
import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.SQLServer2012Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractStoredProcedureTest extends BaseEntityManagerFunctionalTestCase {

	protected void doInAutoCommit(Consumer<Statement> consumer, Map settings) {
		StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();
		if ( settings != null ) {
			ssrb.applySettings( settings );
		}
		StandardServiceRegistry ssr = ssrb.build();

		try {
			try (Connection connection = ssr.getService( JdbcServices.class )
					.getBootstrapJdbcConnectionAccess()
					.obtainConnection();
				 Statement statement = connection.createStatement()) {
				connection.setAutoCommit( true );
				consumer.accept( statement );
			}
			catch (SQLException e) {
				log.debug( e.getMessage() );
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	protected void doInAutoCommit(Consumer<Statement> consumer) {
		doInAutoCommit( consumer, null );
	}
}