package org.hibernate.orm.test.write;

import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

import static org.assertj.core.api.Assertions.assertThat;

/// Enabled H2 counterpart of the compatibility fixture; inherits its unchanged
/// generation, immutability and mutation assertions and original model.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"))
class EnabledCustomSqlGeneratedUpdateTest extends CustomSqlGeneratedUpdateTest {

	@BeforeEach
	void preservationIsEnabled(SessionFactoryScope scope) {
		assertThat( scope.getSessionFactory().getSessionFactoryOptions().isTemporalPreservationEnabled() ).isTrue();
	}
}
