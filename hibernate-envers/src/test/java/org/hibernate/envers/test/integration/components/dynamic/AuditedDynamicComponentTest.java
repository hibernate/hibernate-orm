package org.hibernate.envers.test.integration.components.dynamic;

import java.io.File;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import org.junit.Test;

import org.hibernate.MappingException;
import org.hibernate.cfg.Configuration;
import org.hibernate.envers.Audited;
import org.hibernate.envers.tools.StringTools;
import org.hibernate.envers.test.AbstractEnversTest;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue( jiraKey = "HHH-8049" )
public class AuditedDynamicComponentTest extends AbstractEnversTest {
	@Test
	public void testAuditedDynamicComponentFailure() throws URISyntaxException {
		final Configuration config = new Configuration();
		final URL hbm = Thread.currentThread().getContextClassLoader().getResource( "mappings/dynamicComponents/mapAudited.hbm.xml" );
		config.addFile( new File( hbm.toURI() ) );

		final String auditStrategy = getAuditStrategy();
		if ( !StringTools.isEmpty( auditStrategy ) ) {
			config.setProperty( "org.hibernate.envers.audit_strategy", auditStrategy );
		}

		final ServiceRegistry serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( config.getProperties() );
		try {
			config.buildSessionFactory( serviceRegistry );
			Assert.fail( "MappingException expected" );
		}
		catch ( MappingException e ) {
			Assert.assertEquals(
					"Audited dynamic-component properties are not supported. Consider applying @NotAudited annotation to "
							+ AuditedDynamicMapComponent.class.getName() + "#customFields.",
					e.getMessage()
			);
		}
		finally {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Audited
	public static class AuditedDynamicMapComponent implements Serializable {
		public long id;
		public String note;
		public Map<String, Object> customFields = new HashMap<String, Object>(); // Invalid audited dynamic-component.
	}
}
