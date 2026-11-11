package org.hibernate.orm.test.envers.integration.reventity;

import java.time.LocalDateTime;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.entities.reventity.CustomLocalDateTimeRevEntity;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.descriptor.DateTimeUtils;
import org.hibernate.type.internal.TemporalMappingValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.Level.SUPPORTED;

/// Independent enabled factory retains revision lookup coverage and verifies the
/// generated local fields after a database load under both audit strategies.
///
/// @author Steve Ebersole
@RequiresDialectFeature(feature = RevisionLocalDateTimeEligibility.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
@Jpa(annotatedClasses = { StrTestEntity.class, CustomLocalDateTimeRevEntity.class }, integrationSettings = {
		@Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"),
		@Setting(name = EnversSettings.REVISION_LISTENER, value = "org.hibernate.orm.test.envers.integration.reventity.PreservedLocalDateTimeTest$Capture")
})
public class PreservedLocalDateTimeTest extends LocalDateTimeTest {
	@Override
	protected boolean preservationEnabled() {
		return true;
	}

	@Test
	void generatedFieldsSurviveDatabaseRead(EntityManagerFactoryScope scope) {
		final var factory = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		final var mapping = factory.getMappingMetamodel().getEntityDescriptor( CustomLocalDateTimeRevEntity.class )
				.findAttributeMapping( "localDateTimestamp" ).getSingleJdbcMapping();
		final var dialect = factory.getJdbcServices().getDialect();
		final var assessment = TemporalMappingValidator.assess( dialect,
				mapping.getJdbcJavaType(), mapping.getJdbcType(), "revision timestamp" );
		System.out.println( "REVISION_TEMPORAL_MAPPING java=" + mapping.getJdbcJavaType().getClass().getName()
				+ " jdbc=" + mapping.getJdbcType().getClass().getName() + " converter=" + mapping.getValueConverter()
				+ " assessment=" + assessment );
		assertThat( assessment.date() ).isEqualTo( SUPPORTED );
		assertThat( assessment.time() ).isEqualTo( SUPPORTED );
		assertThat( Capture.generated ).isNotNull();
		final var expected = DateTimeUtils.adjustToDefaultPrecision( Capture.generated, dialect );
		scope.inEntityManager( em -> assertThat( AuditReaderFactory.get( em )
				.findRevision( CustomLocalDateTimeRevEntity.class, 1 ).getLocalDateTimestamp() ).isEqualTo( expected ) );
	}

	public static class Capture implements RevisionListener {
		// Each class-template invocation generates its own revision before its tests.
		private static LocalDateTime generated;

		@Override
		public void newRevision(Object entity) {
			generated = ((CustomLocalDateTimeRevEntity) entity).getLocalDateTimestamp();
		}
	}
}
