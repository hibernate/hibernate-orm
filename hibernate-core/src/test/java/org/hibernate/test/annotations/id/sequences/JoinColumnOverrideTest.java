//$Id$
package org.hibernate.test.annotations.id.sequences;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jboss.logging.Logger;
import org.junit.Test;

import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.test.annotations.id.sequences.entities.Bunny;
import org.hibernate.test.annotations.id.sequences.entities.PointyTooth;
import org.hibernate.test.annotations.id.sequences.entities.TwinkleToes;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests for JIRA issue ANN-748.
 *
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
public class JoinColumnOverrideTest extends BaseUnitTestCase {
	private static final Logger log = Logger.getLogger( JoinColumnOverrideTest.class );

	@Test
	@TestForIssue( jiraKey = "ANN-748" )
	public void testBlownPrecision() throws Exception {
		try {
			Configuration config = new Configuration();
			config.addAnnotatedClass(Bunny.class);
			config.addAnnotatedClass(PointyTooth.class);
			config.addAnnotatedClass(TwinkleToes.class);
			config.buildSessionFactory( ServiceRegistryBuilder.buildServiceRegistry( config.getProperties() ) );
			String[] schema = config.generateSchemaCreationScript( new SQLServerDialect() );
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
		catch (Exception e) {
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
            log.debug(writer.toString());
			fail(e.getMessage());
		}
	}
}
