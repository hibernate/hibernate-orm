/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.java;

import java.util.Date;
import java.util.TimeZone;

import org.hibernate.type.descriptor.java.JdbcTimeJavaType;

import org.junit.After;
import org.junit.Before;

/**
 * @author Owen Farrell
 */
public class JdbcTimeJavaTypeDescriptorTest extends AbstractDescriptorTest<Date> {
	final Date original = new Date();
	final Date copy = new Date( original.getTime() );
	final Date different = new Date( original.getTime() + 500L);

	private TimeZone originalTimeZone;

	public JdbcTimeJavaTypeDescriptorTest() {
		super( JdbcTimeJavaType.INSTANCE );
	}

	@Override
	protected Data<Date> getTestData() {
		return new Data<Date>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return true;
	}

	@Before
	public void changeTimeZone() {
		originalTimeZone = TimeZone.getDefault();
		TimeZone.setDefault( TimeZone.getTimeZone( "Africa/Monrovia" ) );
	}

	@After
	public void restoreTimeZone() {
		TimeZone.setDefault( originalTimeZone );
	}
}
