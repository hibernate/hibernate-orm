/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Properties;

import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.model.relational.SimpleAuxiliaryDatabaseObject;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.SessionImpl;
import org.hibernate.jdbc.Work;
import org.hibernate.type.StandardBasicTypes;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.env.TestingDatabaseInfo;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.boot.BasicTestingJdbcServiceImpl;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * I went back to 3.3 source and grabbed the code/logic as it existed back then and crafted this
 * unit test so that we can make sure the value keep being generated in the expected manner
 *
 * @author Steve Ebersole
 */
@SuppressWarnings({ "deprecation" })
@RequiresDialectFeature( DialectChecks.SupportsSequences.class )
public class SequenceHiLoGeneratorNoIncrementTest extends BaseUnitTestCase {
	private static final String TEST_SEQUENCE = "test_sequence";

	private StandardServiceRegistry serviceRegistry;
	private SessionFactoryImplementor sessionFactory;
	private SequenceHiLoGenerator generator;
    private SessionImplementor session;

	@Before
	public void setUp() throws Exception {
		BasicTestingJdbcServiceImpl jdbcServices = new BasicTestingJdbcServiceImpl();
		jdbcServices.prepare( false );

		serviceRegistry = new StandardServiceRegistryBuilder()
				.enableAutoClose()
				.addService( JdbcEnvironment.class, jdbcServices.getJdbcEnvironment() )
				.addService( JdbcServices.class, jdbcServices )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.build();

		generator = new SequenceHiLoGenerator();

		// Build the properties used to configure the id generator
		Properties properties = new Properties();
		properties.setProperty( SequenceGenerator.SEQUENCE, TEST_SEQUENCE );
		properties.setProperty( SequenceHiLoGenerator.MAX_LO, "0" ); // JPA allocationSize of 1
		properties.put(
				PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER,
				new ObjectNameNormalizer() {
					@Override
					protected MetadataBuildingContext getBuildingContext() {
						return new MetadataBuildingContextTestingImpl( serviceRegistry );
					}
				}
		);
		generator.configure( StandardBasicTypes.LONG, properties, serviceRegistry );

		final Metadata metadata = new MetadataSources( serviceRegistry ).buildMetadata();
		metadata.getDatabase().addAuxiliaryDatabaseObject(
				new SimpleAuxiliaryDatabaseObject(
						Collections.<String>emptySet(),
						null,
						null,
						generator.sqlCreateStrings( TestingDatabaseInfo.DIALECT ),
						generator.sqlDropStrings( TestingDatabaseInfo.DIALECT )
				)
		);

		sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();
	}

	@After
	public void tearDown() throws Exception {
        if(session != null && !session.isClosed()) {
            ((Session)session).close();
        }
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
		if ( serviceRegistry != null ) {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testHiLoAlgorithm() {
		session = (SessionImpl) sessionFactory.openSession();
		((Session)session).beginTransaction();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// initially sequence should be uninitialized
		assertEquals( 0L, extractSequenceValue( (session) ) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// historically the hilo generators skipped the initial block of values;
		// 		so the first generated id value is maxlo + 1, here be 4
		Long generatedValue = (Long) generator.generate( session, null );
		assertEquals( 1L, generatedValue.longValue() );
		// which should also perform the first read on the sequence which should set it to its "start with" value (1)
		assertEquals( 1L, extractSequenceValue( (session) ) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		generatedValue = (Long) generator.generate( session, null );
		assertEquals( 2L, generatedValue.longValue() );
		assertEquals( 2L, extractSequenceValue( (session) ) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		generatedValue = (Long) generator.generate( session, null );
		assertEquals( 3L, generatedValue.longValue() );
		assertEquals( 3L, extractSequenceValue( (session) ) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		generatedValue = (Long) generator.generate( session, null );
		assertEquals( 4L, generatedValue.longValue() );
		assertEquals( 4L, extractSequenceValue( (session) ) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		generatedValue = (Long) generator.generate( session, null );
		assertEquals( 5L, generatedValue.longValue() );
		assertEquals( 5L, extractSequenceValue( (session) ) );

		((Session)session).getTransaction().commit();
		((Session)session).close();
	}

	private long extractSequenceValue(final SessionImplementor session) {
		class WorkImpl implements Work {
			private long value;
			public void execute(Connection connection) throws SQLException {
				
				PreparedStatement query = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( "select currval('" + TEST_SEQUENCE + "');" );
				ResultSet resultSet = session.getJdbcCoordinator().getResultSetReturn().extract( query );
				resultSet.next();
				value = resultSet.getLong( 1 );
			}
		}
		WorkImpl work = new WorkImpl();
		( (Session) session ).doWork( work );
		return work.value;
	}
}
