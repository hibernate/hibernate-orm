/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.descriptor.java;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.hibernate.type.descriptor.java.ZonedDateTimeJavaDescriptor;

/**
 * @author Jordan Gigov
 */
public class ZonedDateTimeDescriptorTest extends AbstractDescriptorTest<ZonedDateTime> {
	final ZonedDateTime original = ZonedDateTime.of( LocalDateTime.of( 2016, 10, 8, 15, 13 ), ZoneId.of( "UTC" ));
	final ZonedDateTime copy = ZonedDateTime.of( LocalDateTime.of( 2016, 10, 8, 15, 13 ), ZoneId.of( "UTC" ) );
	final ZonedDateTime different = ZonedDateTime.of( LocalDateTime.of( 2016, 10, 8, 15, 13 ), ZoneId.of( "EET" ) );

	public ZonedDateTimeDescriptorTest() {
		super(ZonedDateTimeJavaDescriptor.INSTANCE);
	}

	@Override
	protected Data<ZonedDateTime> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}

}
