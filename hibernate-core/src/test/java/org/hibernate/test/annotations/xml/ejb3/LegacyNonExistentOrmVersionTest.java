/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.xml.ejb3;

import org.hibernate.InvalidMappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.annotations.reflection.JPAOverriddenAnnotationReader;
import org.hibernate.internal.util.xml.UnsupportedOrmXsdVersionException;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Equivalent to {@link org.hibernate.test.annotations.xml.ejb3.NonExistentOrmVersionTest}
 * for the legacy {@link JPAOverriddenAnnotationReader}.
 *
 * @author Emmanuel Bernard
 * @deprecated This test will be removed in Hibernate ORM 6, along with the legacy {@link JPAOverriddenAnnotationReader}.
 */
@TestForIssue(jiraKey = "HHH-6271")
public class LegacyNonExistentOrmVersionTest extends BaseUnitTestCase {
	@Test
	public void testNonExistentOrmVersion() {
		try {
			new MetadataSources()
					.addResource( "org/hibernate/test/annotations/xml/ejb3/orm5.xml" )
					.buildMetadata();
			fail( "Expecting failure due to unsupported xsd version" );
		}
		catch ( InvalidMappingException expected ) {
		}
		catch ( UnsupportedOrmXsdVersionException expected ) {
		}
	}
}
