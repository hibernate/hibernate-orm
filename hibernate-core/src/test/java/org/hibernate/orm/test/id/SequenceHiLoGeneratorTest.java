/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.id;

import java.util.Properties;

import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.SequenceGenerator;
import org.hibernate.id.SequenceHiLoGenerator;
import org.hibernate.type.StandardBasicTypes;

import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks.SupportsSequences;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * I went back to 3.3 source and grabbed the code/logic as it existed back then and crafted this
 * unit test so that we can make sure the value keep being generated in the expected manner
 *
 * @author Steve Ebersole
 */
@SuppressWarnings({"deprecation"})
@RequiresDialectFeature(feature = SupportsSequences.class)
@BaseUnitTest
public class SequenceHiLoGeneratorTest {
	private static final String TEST_SEQUENCE = "test_sequence";

	private StandardServiceRegistry serviceRegistry;
	private SessionFactoryImplementor sessionFactory;
	private SequenceHiLoGenerator generator;
	private SequenceValueExtractor sequenceValueExtractor;

	@BeforeEach
	public void setUp() throws Exception {
		serviceRegistry = new StandardServiceRegistryBuilder()
				.enableAutoClose()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.build();

		MetadataBuildingContext buildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );

		Properties properties = new Properties();
		properties.setProperty( SequenceGenerator.SEQUENCE, TEST_SEQUENCE );
		properties.setProperty( SequenceHiLoGenerator.MAX_LO, "3" );
		properties.put(
				PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER,
				buildingContext.getObjectNameNormalizer()
		);

		generator = new SequenceHiLoGenerator();
		generator.configure( StandardBasicTypes.LONG, properties, serviceRegistry );

		Metadata metadata = new MetadataSources( serviceRegistry ).buildMetadata();
		generator.registerExportables( metadata.getDatabase() );

		sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();
		sequenceValueExtractor = new SequenceValueExtractor( sessionFactory.getDialect(), TEST_SEQUENCE );
	}

	@AfterEach
	public void tearDown() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
		if ( serviceRegistry != null ) {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testHiLoAlgorithm() {
		TransactionUtil.doInHibernate(
				() -> sessionFactory,
				session -> {
					// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					// historically the hilo generators skipped the initial block of values;
					// so the first generated id value is maxlo + 1, here be 4
					assertEquals( 4L, generateValue( session ) );
					// which should also perform the first read on the sequence which should set it to its "start with" value (1)
					assertEquals( 1L, extractSequenceValue( session ) );

					// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					assertEquals( 5L, generateValue( session ) );
					assertEquals( 1L, extractSequenceValue( session ) );

					// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					assertEquals( 6L, generateValue( session ) );
					assertEquals( 1L, extractSequenceValue( session ) );

					// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					assertEquals( 7L, generateValue( session ) );
					// unlike the newer strategies, the db value will not get update here. It gets updated on the next invocation
					// after a clock over
					assertEquals( 1L, extractSequenceValue( session ) );

					// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					assertEquals( 8L, generateValue( session ) );
					// this should force an increment in the sequence value
					assertEquals( 2L, extractSequenceValue( session ) );
				}
		);
	}

	private long extractSequenceValue(Session session) {
		return sequenceValueExtractor.extractSequenceValue( (SessionImplementor) session );
	}

	private long generateValue(Session session) {
		return (Long) generator.generate( (SharedSessionContractImplementor) session, null );
	}
}
