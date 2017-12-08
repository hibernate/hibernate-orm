/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.strategy;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.envers.strategy.DefaultAuditStrategy;
import org.hibernate.envers.strategy.ValidityAuditStrategy;
import org.hibernate.service.ServiceRegistry;
import org.junit.jupiter.api.Test;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12077")
public class AuditStrategySelectorTest {

	@Test
	public void testAuditStrategySelectorNoneSpecified() {
		testAuditStrategySelector( null, DefaultAuditStrategy.class );
	}

	@Test
	public void testAuditStrategySelectorDefaultSpecified() {
		testAuditStrategySelector( "default", DefaultAuditStrategy.class );
		testAuditStrategySelector( DefaultAuditStrategy.class.getSimpleName(), DefaultAuditStrategy.class );
		testAuditStrategySelector( DefaultAuditStrategy.class.getName(), DefaultAuditStrategy.class );
	}

	@Test
	public void testAuditStrategySelectorValiditySpecified() {
		testAuditStrategySelector( "validity", ValidityAuditStrategy.class );
		testAuditStrategySelector( ValidityAuditStrategy.class.getSimpleName(), ValidityAuditStrategy.class );
		testAuditStrategySelector( ValidityAuditStrategy.class.getName(), ValidityAuditStrategy.class );
	}

	private void testAuditStrategySelector(String propertyValue, Class<? extends AuditStrategy> expectedStrategyClass) {
		final Map properties = new HashMap<>();
		if ( propertyValue != null ) {
			properties.put( EnversSettings.AUDIT_STRATEGY, propertyValue );
		}

		final ServiceRegistry sr = ServiceRegistryBuilder.buildServiceRegistry( properties );
		try {
			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( sr ).buildMetadata();
			assertTyping(
					expectedStrategyClass,
					metadata.getAuditMetadataBuilder().getAuditMetadataBuildingOptions().getAuditStrategy()
			);
		}
		finally {
			ServiceRegistryBuilder.destroy( sr );
		}
	}
}
