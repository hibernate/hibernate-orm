/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.fetching;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.userguide.model.Image;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class LazyBasicAttributeTest extends BaseEntityManagerFunctionalTestCase {

	private static final Logger LOGGER = Logger.getLogger( LazyBasicAttributeTest.class );

	private PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider();

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Image.class,
		};
	}

	@Override
	protected Map buildSettings() {
		Map settings = super.buildSettings();
		settings.put(
				AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
		return settings;
	}

	@Override
	public void releaseResources() {
		super.releaseResources();
		connectionProvider.stop();
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Image image = new Image();
			image.setId(1L);
			image.setContent(new byte[] {1, 2, 3});
			entityManager.persist(image);
		});
		doInJPA( this::entityManagerFactory, entityManager -> {
			connectionProvider.clear();
			Image image = entityManager.find(Image.class, 1L);
			LOGGER.debug("Fetched image");
			List<PreparedStatement> preparedStatements = connectionProvider.getPreparedStatements();
			assertEquals( 1, preparedStatements.size() );
			assertNotNull( connectionProvider.getPreparedStatement( "select image0_.id as id1_0_0_ from Image image0_ where image0_.id=?" ) );
			assertArrayEquals(new byte[] {1, 2, 3}, image.getContent());
			preparedStatements = connectionProvider.getPreparedStatements();
			assertEquals( 2, preparedStatements.size() );
			assertNotNull( connectionProvider.getPreparedStatement( "select image_.content as content2_0_ from Image image_ where image_.id=?" ) );
		});
	}
}
