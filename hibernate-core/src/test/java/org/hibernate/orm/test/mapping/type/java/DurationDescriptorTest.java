/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;

import java.time.Duration;
import org.hibernate.type.descriptor.java.DurationJavaType;

/**
 * @author Jordan Gigov
 */
public class DurationDescriptorTest extends AbstractDescriptorTest<Duration> {
	final Duration original = Duration.ofSeconds( 3621, 256);
	final Duration copy = Duration.ofSeconds( 3621, 256);
	final Duration different = Duration.ofSeconds( 1621, 156);

	public DurationDescriptorTest() {
		super( DurationJavaType.INSTANCE);
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
