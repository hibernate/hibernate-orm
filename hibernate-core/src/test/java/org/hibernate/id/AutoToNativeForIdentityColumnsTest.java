/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.sql.PreparedStatement;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialectFeature(DialectChecks.SupportsOnlyIdentityColumns.class)
public class AutoToNativeForIdentityColumnsTest extends
		BaseNonConfigCoreFunctionalTestCase {

	private PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider();

	@Override
	protected void addSettings(Map settings) {
		settings.put(
				AvailableSettings.CONNECTION_PROVIDER, connectionProvider
		);
	}

	@Override
	protected void releaseResources() {
		super.releaseResources();
		connectionProvider.stop();
	}

	@Test
	public void test() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			IdHolder holder = new IdHolder();
			session.persist( holder );
		} );
		PreparedStatement preparedStatement = connectionProvider.getPreparedStatement(
				"insert into IdHolder values ( )" );
		verify( preparedStatement, times( 1 ) ).executeUpdate();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				IdHolder.class
		};
	}

	@Entity(name = "IdHolder")
	public static class IdHolder {

		@Id
		@Column(name = "id")
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Integer id;
	}

}
