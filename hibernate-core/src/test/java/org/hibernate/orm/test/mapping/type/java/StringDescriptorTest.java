/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.java;


import org.hibernate.type.descriptor.java.StringJavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class StringDescriptorTest extends AbstractDescriptorTest<String> {
	final String original = "abc";
	final String copy = new String( original.toCharArray() );
	final String different = "xyz";

	public StringDescriptorTest() {
		super( StringJavaTypeDescriptor.INSTANCE );
	}

	@Override
	protected Data<String> getTestData() {
		return new Data<String>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}
}
