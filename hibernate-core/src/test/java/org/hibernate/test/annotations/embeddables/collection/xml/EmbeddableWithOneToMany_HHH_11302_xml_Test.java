/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embeddables.collection.xml;

import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-11302")
public class EmbeddableWithOneToMany_HHH_11302_xml_Test extends
		BaseEntityManagerFunctionalTestCase {

	PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider();

	@Override
	public String[] getEjb3DD() {
		return new String[] {
				"org/hibernate/test/annotations/embeddables/collection/orm.xml"
		};
	}

	public void buildEntityManagerFactory() throws Exception {
		try {
			super.buildEntityManagerFactory();
			fail( "Should throw AnnotationException!" );
		}
		catch ( AnnotationException expected ) {
			assertTrue( expected.getMessage().startsWith(
					"@OneToMany, @ManyToMany or @ElementCollection cannot be used inside an @Embeddable that is also contained within an @ElementCollection"
			) );
		}
		finally {
			connectionProvider.stop();
		}
	}

	protected Map buildSettings() {
		Map settings = super.buildSettings();
		settings.put( AvailableSettings.CONNECTION_PROVIDER, connectionProvider );
		return settings;
	}

	@Test
	public void test() {
	}

}
