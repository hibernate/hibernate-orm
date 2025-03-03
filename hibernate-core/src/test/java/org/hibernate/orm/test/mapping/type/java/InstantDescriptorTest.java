/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;

import java.time.Instant;
import org.hibernate.type.descriptor.java.InstantJavaType;

/**
 * @author Jordan Gigov
 */
public class InstantDescriptorTest extends AbstractDescriptorTest<Instant> {
	final Instant original = Instant.ofEpochMilli( 1476340818745L );
	final Instant copy = Instant.ofEpochMilli( 1476340818745L );
	final Instant different = Instant.ofEpochMilli( 1476340818746L );

	public InstantDescriptorTest() {
		super( InstantJavaType.INSTANCE);
	}

	@Override
	protected Data<Instant> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}

}
