//$Id$
package org.hibernate.test.annotations.id;

import static org.hibernate.aTestLogger.LOG;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.annotations.id.entities.Bunny;
import org.hibernate.test.annotations.id.entities.PointyTooth;
import org.hibernate.test.annotations.id.entities.TwinkleToes;

/**
 * Tests for JIRA issue ANN-748.
 *
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
public class JoinColumnOverrideTest extends TestCase {

	public JoinColumnOverrideTest(String x) {
		super(x);
	}

	public void testBlownPrecision() throws Exception {

		try {
			AnnotationConfiguration config = new AnnotationConfiguration();
			config.addAnnotatedClass(Bunny.class);
			config.addAnnotatedClass(PointyTooth.class);
			config.addAnnotatedClass(TwinkleToes.class);
			config.buildSessionFactory( getServiceRegistry() );
			String[] schema = config
					.generateSchemaCreationScript(new SQLServerDialect());
			for (String s : schema) {
                LOG.debug(s);
			}
			String expectedSqlPointyTooth = "create table PointyTooth (id numeric(128,0) not null, " +
					"bunny_id numeric(128,0) null, primary key (id))";
			assertEquals("Wrong SQL", expectedSqlPointyTooth, schema[1]);

			String expectedSqlTwinkleToes = "create table TwinkleToes (id numeric(128,0) not null, " +
			"bunny_id numeric(128,0) null, primary key (id))";
			assertEquals("Wrong SQL", expectedSqlTwinkleToes, schema[2]);
		} catch (Exception e) {
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
            LOG.debug(writer.toString());
			fail(e.getMessage());
		}
	}

	/**
	 * @see org.hibernate.test.annotations.TestCase#getAnnotatedClasses()
	 */
	@Override
    protected Class[] getAnnotatedClasses() {
		return new Class[] {};
	}
}
