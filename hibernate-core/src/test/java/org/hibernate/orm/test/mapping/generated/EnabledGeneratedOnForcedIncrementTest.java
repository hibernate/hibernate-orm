package org.hibernate.orm.test.mapping.generated;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

import static org.assertj.core.api.Assertions.assertThat;

/// Enabled H2 counterpart of the compatibility fixture; inherits its unchanged
/// generation, immutability and mutation assertions and original model.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@Jpa(
		properties = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"),
		annotatedClasses = GeneratedOnForcedIncrementTest.WithUpdateTimestamp.class
)
class EnabledGeneratedOnForcedIncrementTest extends GeneratedOnForcedIncrementTest {

	@BeforeEach
	void preservationIsEnabled(EntityManagerFactoryScope scope) {
		assertThat( scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class ).getSessionFactoryOptions().isTemporalPreservationEnabled() ).isTrue();
	}
}
