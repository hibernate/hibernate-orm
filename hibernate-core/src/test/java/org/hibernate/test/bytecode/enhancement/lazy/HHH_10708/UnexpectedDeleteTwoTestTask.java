/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.HHH_10708;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.junit.Assert;


public class UnexpectedDeleteTwoTestTask extends AbstractEnhancerTestTask {

	private BarTwo myBar;

	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {FooTwo.class, BarTwo.class};
	}

	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( AvailableSettings.SHOW_SQL, Boolean.FALSE.toString() );
		cfg.setProperty( AvailableSettings.USE_SQL_COMMENTS, Boolean.TRUE.toString() );
		cfg.setProperty( AvailableSettings.USE_SQL_COMMENTS, Boolean.TRUE.toString() );
		super.prepare( cfg );

		Session s = getFactory().openSession();
		s.beginTransaction();

		BarTwo bar = new BarTwo();
		FooTwo foo1 = new FooTwo();
		FooTwo foo2 = new FooTwo();
		s.save( bar );
		s.save( foo1 );
		s.save( foo2 );

		bar.foos.add( foo1 );
		bar.foos.add( foo2 );

		s.getTransaction().commit();
		s.clear();
		s.close();

		myBar = bar;
	}

	public void execute() {
		Session s = getFactory().openSession();
		s.beginTransaction();

		s.refresh( myBar );
		Assert.assertFalse( myBar.foos.isEmpty() );

		// The issue is that currently, for some unknown reason, foos are deleted on flush

		s.flush();
		s.getTransaction().commit();
		s.close();

		s = getFactory().openSession();
		s.beginTransaction();

		BarTwo bar = s.get( BarTwo.class, myBar.id );
		Assert.assertFalse( bar.foos.isEmpty() );

		s.flush();
		s.getTransaction().commit();
		s.close();
	}

	protected void cleanup() {
	}

}