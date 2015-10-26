/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hbm.version;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class GeneratedVersionBindingTest extends BaseUnitTestCase {
	@Test
	public void testIt() {
		final Metadata metadata = new MetadataSources()
				.addResource("org/hibernate/test/hbm/version/Mappings.hbm.xml")
				.buildMetadata();
	}
}
