/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.boot.model.source;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Guillaume Smet
 */
public class AttributePathTest {

	@Test
	@TestForIssue(jiraKey = "HHH-10863")
	public void testCollectionElement() {
		AttributePath attributePath = AttributePath.parse( "items.collection&&element.name" );

		assertFalse( attributePath.isCollectionElement() );
		assertTrue( attributePath.getParent().isCollectionElement() );
		assertFalse( attributePath.getParent().getParent().isCollectionElement() );
	}

}
