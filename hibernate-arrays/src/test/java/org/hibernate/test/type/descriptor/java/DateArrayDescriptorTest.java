/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.descriptor.java;

import java.util.Date;
import org.hibernate.type.ArrayTypes;

/**
 * @author Jordan Gigov
 */
public class DateArrayDescriptorTest extends AbstractDescriptorTest<Date[]> {
	final Date[] original = new Date[]{ new Date(1475934200000l), new Date(1472934200000l), new Date(1375934200000l) };
	final Date[] copy = new Date[]{ new Date(1475934200000l), new Date(1472934200000l), new Date(1375934200000l) };
	final Date[] different = new Date[]{ new Date(1375934200000l), new Date(1412934200000l), new Date(1375934200000l) };

	public DateArrayDescriptorTest() {
		super(ArrayTypes.DATE.getJavaTypeDescriptor());
	}

	@Override
	protected Data<Date[]> getTestData() {
		return new Data<Date[]>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return true;
	}
	
}
