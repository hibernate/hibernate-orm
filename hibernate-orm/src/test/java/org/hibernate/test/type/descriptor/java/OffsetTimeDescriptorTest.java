/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.descriptor.java;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import org.hibernate.type.descriptor.java.OffsetTimeJavaDescriptor;

/**
 * @author Jordan Gigov
 */
public class OffsetTimeDescriptorTest extends AbstractDescriptorTest<OffsetTime> {
	final OffsetTime original = OffsetTime.of(LocalTime.of( 15, 13 ), ZoneOffset.ofHoursMinutes( -6, 0));
	final OffsetTime copy = OffsetTime.of(LocalTime.of( 15, 13 ), ZoneOffset.ofHoursMinutes( -6, 0));
	final OffsetTime different = OffsetTime.of(LocalTime.of( 15, 13 ), ZoneOffset.ofHoursMinutes( 4, 30));

	public OffsetTimeDescriptorTest() {
		super(OffsetTimeJavaDescriptor.INSTANCE);
	}

	@Override
	protected Data<OffsetTime> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}
	
}
