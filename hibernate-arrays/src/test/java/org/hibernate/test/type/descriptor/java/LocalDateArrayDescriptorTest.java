/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.descriptor.java;

import java.time.LocalDate;
import org.hibernate.type.ArrayTypes;
import org.hibernate.type.LocalDateType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Jordan Gigov
 */
public class LocalDateArrayDescriptorTest extends AbstractDescriptorTest<LocalDate[]> {
	final LocalDate[] original;
	final LocalDate[] copy;
	final LocalDate[] different;

	public LocalDateArrayDescriptorTest() {
		super(ArrayTypes.LOCAL_DATE.getJavaTypeDescriptor());
		JavaTypeDescriptor<LocalDate> jt = LocalDateType.INSTANCE.getJavaTypeDescriptor();
		original = new LocalDate[] {
			LocalDate.of( 2016, 10, 8 ),
			LocalDate.of( 2016, 9,  3 ),
			LocalDate.of( 2013, 8,  8 )
		};
		copy = new LocalDate[] {
			LocalDate.of( 2016, 10, 8 ),
			LocalDate.of( 2016, 9,  3 ),
			LocalDate.of( 2013, 8,  8 )
		};
		different = new LocalDate[] {
			LocalDate.of( 2016, 10, 8 ),
			LocalDate.of( 2016,  9, 7 ),
			LocalDate.of( 2013,  8, 8 )
		};
	}

	@Override
	protected Data<LocalDate[]> getTestData() {
		return new Data<LocalDate[]>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}
	
}
