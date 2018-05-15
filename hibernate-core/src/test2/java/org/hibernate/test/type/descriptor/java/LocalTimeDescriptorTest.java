/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.descriptor.java;

import java.time.LocalDate;
import org.hibernate.type.descriptor.java.LocalDateJavaDescriptor;

/**
 * @author Jordan Gigov
 */
public class LocalTimeDescriptorTest extends AbstractDescriptorTest<LocalDate> {
	final LocalDate original = LocalDate.of( 2016, 10, 8 );
	final LocalDate copy = LocalDate.of( 2016, 10, 8 );
	final LocalDate different = LocalDate.of( 2013,  8, 8 );

	public LocalTimeDescriptorTest() {
		super(LocalDateJavaDescriptor.INSTANCE);
	}

	@Override
	protected Data<LocalDate> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}
	
}
