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

public class UnexpectedDeleteOneTestTask extends AbstractEnhancerTestTask {

	private int fooId;

	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {FooOne.class, BarOne.class};
	}

	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( AvailableSettings.SHOW_SQL, Boolean.FALSE.toString() );
		cfg.setProperty( AvailableSettings.USE_SQL_COMMENTS, Boolean.TRUE.toString() );
		super.prepare( cfg );

		Session s = getFactory().openSession();
		s.beginTransaction();

		BarOne bar1 = new BarOne();
		BarOne bar2 = new BarOne();
		FooOne foo = new FooOne();
		s.save( bar1 );
		s.save( bar2 );
		s.save( foo );
		bar1.foo = foo;
		bar2.foo = foo;
		fooId = foo.id;

		s.getTransaction().commit();
		s.clear();
		s.close();
	}

	public void execute() {
		Session s = getFactory().openSession();
		s.beginTransaction();

		FooOne foo = s.get( FooOne.class, fooId );

		// accessing the collection results in an exception
		foo.bars.size();

		s.flush();
		s.getTransaction().commit();
		s.close();
	}

	protected void cleanup() {
	}

}