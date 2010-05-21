/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.id;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import junit.framework.TestCase;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.TestingDatabaseInfo;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.impl.SessionImpl;
import org.hibernate.jdbc.Work;
import org.hibernate.mapping.SimpleAuxiliaryDatabaseObject;

/**
 * I went back to 3.3 source and grabbed the code/logic as it existed back then and crafted this
 * unit test so that we can make sure the value keep being generated in the expected manner
 *
 * @author Steve Ebersole
 */
@SuppressWarnings({ "deprecation" })
public class TableHiLoGeneratorTest extends TestCase {
	private static final String GEN_TABLE = "generator_table";
	private static final String GEN_COLUMN = TableHiLoGenerator.DEFAULT_COLUMN_NAME;

	private Configuration cfg;
	private SessionFactoryImplementor sessionFactory;
	private TableHiLoGenerator generator;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		Properties properties = new Properties();
		properties.setProperty( TableHiLoGenerator.TABLE, GEN_TABLE );
		properties.setProperty( TableHiLoGenerator.COLUMN, GEN_COLUMN );
		properties.setProperty( TableHiLoGenerator.MAX_LO, "3" );
		properties.put(
				PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER,
				new ObjectNameNormalizer() {
					@Override
					protected boolean isUseQuotedIdentifiersGlobally() {
						return false;
					}

					@Override
					protected NamingStrategy getNamingStrategy() {
						return cfg.getNamingStrategy();
					}
				}
		);

		Dialect dialect = new H2Dialect();

		generator = new TableHiLoGenerator();
		generator.configure( Hibernate.LONG, properties, dialect );

		cfg = TestingDatabaseInfo.buildBaseConfiguration()
				.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		cfg.addAuxiliaryDatabaseObject(
				new SimpleAuxiliaryDatabaseObject(
						generator.sqlCreateStrings( dialect )[0],
						generator.sqlDropStrings( dialect )[0]
				)
		);

		cfg.addAuxiliaryDatabaseObject(
				new SimpleAuxiliaryDatabaseObject(
						generator.sqlCreateStrings( dialect )[1],
						null
				)
		);

		sessionFactory = (SessionFactoryImplementor) cfg.buildSessionFactory();
	}

	@Override
	protected void tearDown() throws Exception {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}

		super.tearDown();
	}

	public void testHiLoAlgorithm() {
		SessionImpl session = (SessionImpl) sessionFactory.openSession();
		session.beginTransaction();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// initially sequence should be uninitialized
		assertEquals( 0L, extractInDatabaseValue( session ) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		Long generatedValue = (Long) generator.generate( session, null );
		assertEquals( 1L, generatedValue.longValue() );
		assertEquals( 1L, extractInDatabaseValue( session ) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		generatedValue = (Long) generator.generate( session, null );
		assertEquals( 2L, generatedValue.longValue() );
		assertEquals( 1L, extractInDatabaseValue( session ) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		generatedValue = (Long) generator.generate( session, null );
		assertEquals( 3L, generatedValue.longValue() );
		assertEquals( 1L, extractInDatabaseValue( session ) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		generatedValue = (Long) generator.generate( session, null );
		assertEquals( 4L, generatedValue.longValue() );
		assertEquals( 2L, extractInDatabaseValue( session ) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		generatedValue = (Long) generator.generate( session, null );
		assertEquals( 5L, generatedValue.longValue() );
		assertEquals( 2L, extractInDatabaseValue( session ) );

		session.getTransaction().commit();
		session.close();
	}

	private long extractInDatabaseValue(Session session) {
		class WorkImpl implements Work {
			private long value;
			public void execute(Connection connection) throws SQLException {
				PreparedStatement query = connection.prepareStatement( "select " + GEN_COLUMN + " from " + GEN_TABLE );
				ResultSet resultSet = query.executeQuery();
				resultSet.next();
				value = resultSet.getLong( 1 );
			}
		}
		WorkImpl work = new WorkImpl();
		session.doWork( work );
		return work.value;
	}
}