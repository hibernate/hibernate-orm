/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.java;


import org.hibernate.type.descriptor.java.BooleanJavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class BooleanDescriptorTest extends AbstractDescriptorTest<Boolean> {
	final Boolean original = Boolean.TRUE;
	final Boolean copy = new Boolean( true );
	final Boolean different = Boolean.FALSE;

	public BooleanDescriptorTest() {
		super( BooleanJavaTypeDescriptor.INSTANCE );
	}

	@Override
	protected Data<Boolean> getTestData() {
		return new Data<Boolean>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}
}
