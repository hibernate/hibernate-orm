/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.descriptor.java;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaDescriptor;

/**
 * @author Jordan Gigov
 */
public class OffsetDateTimeDescriptorTest extends AbstractDescriptorTest<OffsetDateTime> {
	final OffsetDateTime original = OffsetDateTime.of(LocalDateTime.of( 2016, 10, 8, 15, 13 ), ZoneOffset.ofHoursMinutes( 2, 0));
	final OffsetDateTime copy = OffsetDateTime.of(LocalDateTime.of( 2016, 10, 8, 15, 13 ), ZoneOffset.ofHoursMinutes( 2, 0));
	final OffsetDateTime different = OffsetDateTime.of(LocalDateTime.of( 2016, 10, 8, 15, 13 ), ZoneOffset.ofHoursMinutes( 4, 30));

	public OffsetDateTimeDescriptorTest() {
		super(OffsetDateTimeJavaDescriptor.INSTANCE);
	}

	@Override
	protected Data<OffsetDateTime> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}
	
}
