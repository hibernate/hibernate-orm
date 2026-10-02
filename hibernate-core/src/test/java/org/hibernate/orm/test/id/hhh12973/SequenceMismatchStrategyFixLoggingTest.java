package org.hibernate.orm.test.id.hhh12973;

import java.util.EnumSet;

import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.enhanced.SequenceGeneratorLogger;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.logger.LoggerInspectionExtension;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import static org.hibernate.testing.logger.LogLevelContext.withLevel;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The FIX diagnostic must retain the mapping's original increment size.
///
/// @author Steve Ebersole
@BaseUnitTest
@JiraKey( "HHH-14701" )
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportPooledSequences.class )
public class SequenceMismatchStrategyFixLoggingTest {

	@RegisterExtension
	public LoggerInspectionExtension logInspection = LoggerInspectionExtension.builder()
			.setLogger( SequenceGeneratorLogger.SEQUENCE_GENERATOR_LOGGER ).build();

	private final Triggerable mismatch = logInspection.watchForLogMessages( "HHH090203:" );

	@Test
	public void testOriginalMappingIncrementInLog() {
		try (var schemaRegistry = ServiceRegistryUtil.serviceRegistry()) {
			final var schemaMetadata = new MetadataSources( schemaRegistry )
					.addAnnotatedClass( DatabaseMapping.class ).buildMetadata();
			final var schemaExport = new SchemaExport();
			schemaExport.create( EnumSet.of( TargetType.DATABASE ), schemaMetadata );
			try (var logLevel = withLevel( SequenceGeneratorLogger.NAME, Logger.Level.TRACE );
					var registry = ServiceRegistryUtil.serviceRegistryBuilder()
							.applySetting( AvailableSettings.HBM2DDL_AUTO, "none" )
							.applySetting( AvailableSettings.SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY, "fix" )
							.build();
					var factory = new MetadataSources( registry ).addAnnotatedClass( EntityMapping.class )
							.buildMetadata().buildSessionFactory()) {
				assertTrue( mismatch.wasTriggered() );
				assertTrue( mismatch.triggerMessage().contains( "[50] in the entity mapping" ), mismatch.triggerMessage() );
				assertTrue( mismatch.triggerMessage().contains( "database sequence increment size is [5]" ), mismatch.triggerMessage() );
			}
			finally {
				schemaExport.drop( EnumSet.of( TargetType.DATABASE ), schemaMetadata );
			}
		}
	}

	@Entity( name = "DatabaseMapping" )
	@Table( name = "hhh14701_entity" )
	public static class DatabaseMapping {
		@Id
		@GeneratedValue( strategy = GenerationType.SEQUENCE, generator = "hhh14701_sequence" )
		@SequenceGenerator( name = "hhh14701_sequence", sequenceName = "hhh14701_sequence", allocationSize = 5 )
		private Long id;
	}

	@Entity( name = "EntityMapping" )
	@Table( name = "hhh14701_entity" )
	public static class EntityMapping {
		@Id
		@GeneratedValue( strategy = GenerationType.SEQUENCE, generator = "hhh14701_sequence" )
		@SequenceGenerator( name = "hhh14701_sequence", sequenceName = "hhh14701_sequence", allocationSize = 50 )
		private Long id;
	}
}
