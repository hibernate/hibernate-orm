/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.descriptor.java;

import org.hibernate.type.ArrayTypes;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * @author Jordan Gigov
 */
public class StringArrayDescriptorTest extends AbstractDescriptorTest<String[]> {

	final String[] original = new String[]{null, "double-quote at end\"", "\' single quote at start", "escape at end\\", "escape and quote at end\\\""};
	final String[] copy = new String[]{null, "double-quote at end\"", "\' single quote at start", "escape at end\\", "escape and quote at end\\\""};
	final String[] different = new String[]{"null", "double-quote at end\"", "\' single quote at start", "escape at end\\", "escape and quote at end\\\""};

	public StringArrayDescriptorTest() {
		super( ArrayTypes.TEXT.getJavaTypeDescriptor() );
	}

	@Override
	protected Data<String[]> getTestData() {
		return new Data<String[]>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}

	@Test
	public void testEmptyArrayExternalization() {
		// ensure the symmetry of toString/fromString
		String[] emptyArray = new String[]{};
		String externalized = ArrayTypes.TEXT.getJavaTypeDescriptor().toString( emptyArray );
		String[] consumed = (String[]) ArrayTypes.TEXT.getJavaTypeDescriptor().fromString( externalized );
		assertTrue( ArrayTypes.TEXT.getJavaTypeDescriptor().areEqual( emptyArray, consumed ) );
	}

}
