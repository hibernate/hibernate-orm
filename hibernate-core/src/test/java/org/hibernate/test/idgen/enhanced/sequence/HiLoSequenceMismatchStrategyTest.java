/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idgen.enhanced.sequence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.id.SequenceMismatchStrategy;
import org.hibernate.id.enhanced.HiLoOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;
import static org.hibernate.testing.junit4.ExtraAssertions.assertClassAssignability;
import static org.junit.Assert.fail;

/**
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-13783" )
@RequiresDialectFeature( DialectChecks.SupportsSequences.class )
public class HiLoSequenceMismatchStrategyTest extends BaseCoreFunctionalTestCase {

	private String sequenceName = "ID_SEQ_HILO_SEQ";

	@Override
	protected String[] getMappings() {
		return new String[] { "idgen/enhanced/sequence/HiLo.hbm.xml" };
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty(AvailableSettings.SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY, SequenceMismatchStrategy.EXCEPTION.toString());
	}

	@Override
	protected void afterConfigurationBuilt(Configuration configuration) {
		DriverManagerConnectionProviderImpl connectionProvider = new DriverManagerConnectionProviderImpl();
		connectionProvider.configure( Environment.getProperties() );

		String[] dropSequenceStatements = getDialect().getDropSequenceStrings( sequenceName );
		String[] createSequenceStatements = getDialect().getCreateSequenceStrings( sequenceName, 1, 1 );

		try ( Connection connection = connectionProvider.getConnection();
				Statement statement = connection.createStatement() ) {
			for ( String dropSequenceStatement : dropSequenceStatements ) {
				statement.execute( dropSequenceStatement );
			}
			for ( String createSequenceStatement : createSequenceStatements ) {
				statement.execute( createSequenceStatement );
			}
		}
		catch (SQLException e) {
			fail( e.getMessage() );
		}
	}

	@AfterClassOnce
	@SuppressWarnings( {"UnusedDeclaration"})
	protected void releaseSessionFactory() {
		DriverManagerConnectionProviderImpl connectionProvider = new DriverManagerConnectionProviderImpl();
		connectionProvider.configure( Environment.getProperties() );

		String[] dropSequenceStatements = getDialect().getDropSequenceStrings( sequenceName );
		try ( Connection connection = connectionProvider.getConnection();
				Statement statement = connection.createStatement() ) {
			for ( String dropSequenceStatement : dropSequenceStatements ) {
				statement.execute( dropSequenceStatement );
			}
		}
		catch (SQLException e) {
			fail( e.getMessage() );
		}
		super.releaseSessionFactory();
	}

	@Test
	public void testSequenceMismatchStrategyNotApplied() {
		EntityPersister persister = sessionFactory().getEntityPersister( Entity.class.getName() );
		assertClassAssignability( SequenceStyleGenerator.class, persister.getIdentifierGenerator().getClass() );

		SequenceStyleGenerator generator = ( SequenceStyleGenerator ) persister.getIdentifierGenerator();
		assertClassAssignability( HiLoOptimizer.class, generator.getOptimizer().getClass() );

		String sequenceName = generator.getDatabaseStructure().getName();
		Assert.assertEquals(this.sequenceName, sequenceName);

		int incrementSize = generator.getOptimizer().getIncrementSize();
		Assert.assertNotEquals(1, incrementSize);
	}

}
