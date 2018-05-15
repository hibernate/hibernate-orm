/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.descriptor.java;

import java.time.Duration;
import org.hibernate.type.descriptor.java.DurationJavaDescriptor;

/**
 * @author Jordan Gigov
 */
public class DurationDescriptorTest extends AbstractDescriptorTest<Duration> {
	final Duration original = Duration.ofSeconds( 3621, 256);
	final Duration copy = Duration.ofSeconds( 3621, 256);
	final Duration different = Duration.ofSeconds( 1621, 156);

	public DurationDescriptorTest() {
		super(DurationJavaDescriptor.INSTANCE);
	}

	@Override
	protected Data<Duration> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}
	
}
