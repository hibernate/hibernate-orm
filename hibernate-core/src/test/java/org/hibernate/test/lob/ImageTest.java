/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.lob;

/**
 * Tests eager materialization and mutation of data mapped by
 * {@link org.hibernate.type.ImageType}.
 *
 * @author Gail Badner
 */
public class ImageTest extends LongByteArrayTest {
	public String[] getMappings() {
		return new String[] { "lob/ImageMappings.hbm.xml" };
	}
}
