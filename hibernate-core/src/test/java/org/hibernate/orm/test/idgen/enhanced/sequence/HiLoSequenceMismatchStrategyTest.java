/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.enhanced.sequence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.id.enhanced.HiLoOptimizer;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Nathan Xu
 */
@JiraKey(value = "HHH-13783")
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsSequences.class )
@ServiceRegistry(
		settings = @Setting( name = AvailableSettings.SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY, value = "EXCEPTION" )
)
@DomainModel( annotatedClasses = HiLoSequenceMismatchStrategyTest.TestEntity.class )
@SessionFactory
public class HiLoSequenceMismatchStrategyTest {

	public final static String sequenceName = "ID_SEQ_HILO_SEQ";

	@BeforeEach
	public void dropDatabaseSequence(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();

		final String[] dropSequenceStatements = dialect.getSequenceSupport().getDropSequenceStrings( sequenceName );
		final String[] createSequenceStatements = dialect.getSequenceSupport().getCreateSequenceStrings( sequenceName, 1, 1 );

		final ConnectionProvider connectionProvider = scope.getSessionFactory()
				.getServiceRegistry()
				.getService( ConnectionProvider.class );
		scope.inSession(
				session -> {
					Connection connection = null;
					final JdbcConnectionAccess jdbcConnectionAccess = session.getJdbcConnectionAccess();
					try {
						connection = jdbcConnectionAccess.obtainConnection();
						try ( Statement statement = connection.createStatement() ) {

							for ( String dropSequenceStatement : dropSequenceStatements ) {
								try {
									statement.execute( dropSequenceStatement );
								}
								catch (SQLException e) {
									System.out.printf( "TEST DEBUG : dropping sequence failed [`%s`] - %s", dropSequenceStatement, e.getMessage() );
									System.out.println();
									e.printStackTrace( System.out );
									// ignore
								}
							}
							// Commit in between because CockroachDB fails to drop and commit schema objects in the same transaction
							connection.commit();

							for ( String createSequenceStatement : createSequenceStatements ) {
								statement.execute( createSequenceStatement );
							}
							connection.commit();
						}
					}
					catch (SQLException e) {
						fail( e.getMessage() );
					}
					finally {
						if ( connection != null ) {
							try {
								jdbcConnectionAccess.releaseConnection( connection );
							}
							catch (SQLException ignore) {
							}
						}
					}
				}
		);
	}

	@Test
	public void testSequenceMismatchStrategyNotApplied(SessionFactoryScope scope) {
		final EntityPersister persister = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor(TestEntity.class.getName());
		assertThat( persister.getIdentifierGenerator(), instanceOf( SequenceStyleGenerator.class ) );

		final SequenceStyleGenerator generator = (SequenceStyleGenerator) persister.getIdentifierGenerator();


		final Optimizer optimizer = generator.getOptimizer();
		assertThat( optimizer, instanceOf( HiLoOptimizer.class ) );
		assertThat( optimizer.getIncrementSize(), not( is( 1 ) ) );

		assertThat( generator.getDatabaseStructure().getPhysicalName().render(), is( sequenceName ) );
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {

		@Id
		@GeneratedValue(generator = "hilo_sequence_generator")
		@GenericGenerator(name = "hilo_sequence_generator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
				@Parameter(name = "sequence_name", value = sequenceName),
				@Parameter(name = "initial_value", value = "1"),
				@Parameter(name = "increment_size", value = "10"),
				@Parameter(name = "optimizer", value = "hilo")
		})
		private Long id;

		private String aString;
	}

}
