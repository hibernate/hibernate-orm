//$Id$
package org.hibernate.test.annotations.id.sequences;

import static org.junit.Assert.assertEquals;

import org.hibernate.boot.registry.internal.BootstrapServiceRegistryImpl;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.test.annotations.id.sequences.entities.Bunny;
import org.hibernate.test.annotations.id.sequences.entities.PointyTooth;
import org.hibernate.test.annotations.id.sequences.entities.TwinkleToes;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.jboss.logging.Logger;
import org.junit.Test;

/**
 * Tests for JIRA issue ANN-748.
 *
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
@FailureExpectedWithNewMetamodel
public class JoinColumnOverrideTest extends BaseUnitTestCase {
	private static final Logger log = Logger.getLogger( JoinColumnOverrideTest.class );

	@Test
	@TestForIssue( jiraKey = "ANN-748" )
	public void testBlownPrecision() throws Exception {
//		Configuration config = new Configuration();
//		config.addAnnotatedClass(Bunny.class);
//		config.addAnnotatedClass(PointyTooth.class);
//		config.addAnnotatedClass(TwinkleToes.class);
//		config.buildMappings( );
//		String[] schema = config.generateSchemaCreationScript( new SQLServerDialect() );
		MetadataSources metadataSources = new MetadataSources( new BootstrapServiceRegistryImpl() );
		metadataSources.addAnnotatedClass(Bunny.class);
		metadataSources.addAnnotatedClass(PointyTooth.class);
		metadataSources.addAnnotatedClass(TwinkleToes.class);
		MetadataImplementor metadata = (MetadataImplementor) metadataSources.buildMetadata();
		SchemaExport exporter = new SchemaExport( metadata );
		String[] schema = exporter.getCreateSqlScripts();
		for (String s : schema) {
            log.debug(s);
		}
		String expectedSqlPointyTooth = "create table PointyTooth (id numeric(128,0) not null, " +
				"bunny_id numeric(128,0), primary key (id))";
		assertEquals("Wrong SQL", expectedSqlPointyTooth, schema[1]);

		String expectedSqlTwinkleToes = "create table TwinkleToes (id numeric(128,0) not null, " +
		"bunny_id numeric(128,0), primary key (id))";
		assertEquals("Wrong SQL", expectedSqlTwinkleToes, schema[2]);
	}
}
