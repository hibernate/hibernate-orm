/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.descriptor.java;

import java.time.Instant;
import org.hibernate.type.descriptor.java.InstantJavaDescriptor;

/**
 * @author Jordan Gigov
 */
public class InstantDescriptorTest extends AbstractDescriptorTest<Instant> {
	final Instant original = Instant.ofEpochMilli( 1476340818745L );
	final Instant copy = Instant.ofEpochMilli( 1476340818745L );
	final Instant different = Instant.ofEpochMilli( 1476340818746L );

	public InstantDescriptorTest() {
		super(InstantJavaDescriptor.INSTANCE);
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
