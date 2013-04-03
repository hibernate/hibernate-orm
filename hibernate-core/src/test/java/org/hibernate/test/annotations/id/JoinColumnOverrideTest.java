/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.id;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jboss.logging.Logger;
import org.junit.Test;

import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.test.annotations.id.entities.Bunny;
import org.hibernate.test.annotations.id.entities.PointyTooth;
import org.hibernate.test.annotations.id.entities.TwinkleToes;
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
			config.buildMappings( );
			String[] schema = config
					.generateSchemaCreationScript(new SQLServerDialect());
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
