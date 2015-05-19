/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.lob;

/**
 * Test eager materialization and mutation data mapped by
 * #{@link org.hibernate.type.TextType}.
 *
 * @author Gail Badner
 */
public class TextTest extends LongStringTest {
	public String[] getMappings() {
		return new String[] { "lob/TextMappings.hbm.xml" };
	}
}
