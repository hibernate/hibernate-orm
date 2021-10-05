/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.Duration;

import org.hibernate.type.descriptor.java.DurationJavaDescriptor;
import org.hibernate.type.descriptor.jdbc.NumericTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class DurationType
		extends AbstractSingleColumnStandardBasicType<Duration> {
	/**
	 * Singleton access
	 */
	public static final DurationType INSTANCE = new DurationType();

	public DurationType() {
		super( NumericTypeDescriptor.INSTANCE, DurationJavaDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return Duration.class.getSimpleName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}
