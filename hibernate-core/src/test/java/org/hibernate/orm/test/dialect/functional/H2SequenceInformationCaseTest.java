package org.hibernate.orm.test.dialect.functional;

import java.util.HashSet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies H2 sequence metadata extraction and pooled generation without relying on identifier folding.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@JiraKey("HHH-15521")
public class H2SequenceInformationCaseTest {
	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testSequenceMetadataAndGeneration(boolean toUpper) throws Exception {
		try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( JdbcSettings.URL, "jdbc:h2:mem:hhh15521_" + toUpper + ";DATABASE_TO_UPPER=" + toUpper )
				.applySetting( SchemaToolingSettings.HBM2DDL_AUTO, "none" )
				.applySetting( MappingSettings.SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY, "EXCEPTION" )
				.build() ) {
			final var connectionProvider = registry.requireService( ConnectionProvider.class );
			final var connection = connectionProvider.getConnection();
			try {
				try ( var statement = connection.createStatement() ) {
					statement.execute( "create schema Master" );
					statement.execute( "create sequence Master.SampleEntitySequence start with 1 increment by 5" );
					statement.execute( "create table Master.SampleEntity (Id bigint primary key, name varchar(255))" );
				}
				connection.commit();
			}
			finally {
				connectionProvider.closeConnection( connection );
			}

			try ( var factory = new MetadataSources( registry )
					.addAnnotatedClass( SampleEntity.class )
					.buildMetadata()
					.buildSessionFactory() ) {
				final var sequence = factory.unwrap( SessionFactoryImplementor.class )
						.getJdbcServices().getJdbcMetadata().getSequenceInformationList()
						.stream()
						.filter( information -> information.getSequenceName().getSequenceName().getText()
								.equalsIgnoreCase( "SampleEntitySequence" ) )
						.findFirst().orElseThrow();
				assertEquals( toUpper ? "MASTER" : "Master", sequence.getSequenceName().getSchemaName().getText() );
				assertEquals( toUpper ? "SAMPLEENTITYSEQUENCE" : "SampleEntitySequence",
						sequence.getSequenceName().getSequenceName().getText() );
				assertEquals( 5L, sequence.getIncrementValue().longValue() );

				final var entities = new SampleEntity[7];
				factory.inTransaction( session -> {
					for ( int i = 0; i < entities.length; i++ ) {
						entities[i] = new SampleEntity( "item_" + i );
						session.persist( entities[i] );
					}
				} );
				final var identifiers = new HashSet<Long>();
				for ( var entity : entities ) {
					assertNotNull( entity.id );
					assertTrue( identifiers.add( entity.id ) );
				}
				factory.inTransaction( session -> {
					for ( var entity : entities ) {
						final var loaded = session.find( SampleEntity.class, entity.id );
						assertNotNull( loaded );
						assertEquals( entity.name, loaded.name );
					}
				} );
			}
		}
	}

	@Entity(name = "H2CaseSampleEntity")
	@Table(name = "SampleEntity", schema = "Master")
	public static class SampleEntity {
		@Id
		@Column(name = "Id")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sample_sequence")
		@SequenceGenerator(name = "sample_sequence", sequenceName = "Master.SampleEntitySequence", allocationSize = 5)
		Long id;

		String name;

		public SampleEntity() {
		}

		SampleEntity(String name) {
			this.name = name;
		}
	}
}
