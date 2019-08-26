package org.hibernate.test.schemaupdate;

import java.util.EnumSet;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Test;

@RequiresDialect(H2Dialect.class)
@TestForIssue(jiraKey = "HHH-13597")
public class H2DialectDataBaseToUpperTest extends BaseUnitTestCase {

	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

	@Test
	public void hibernateShouldStartUpWithH2AutoUpdateAndDatabaseToUpperFalse() {
		setUp( "false" );
		new SchemaUpdate().setHaltOnError( true )
				.execute( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	@Test
	public void hibernateShouldStartUpWithH2AutoUpdateAndDatabaseToUpperTrue() {
		setUp( "true" );
		new SchemaUpdate().setHaltOnError( true )
				.execute( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	private void setUp(String databaseToUpper) {
		ssr = new StandardServiceRegistryBuilder()
				.applySetting(
						AvailableSettings.URL,
						"jdbc:h2:mem:databaseToUpper;DATABASE_TO_UPPER=" + databaseToUpper
				)
				.build();
		final MetadataSources metadataSources = new MetadataSources( ssr );
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
	}

	@After
	public void tearDown() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
