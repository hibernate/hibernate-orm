/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.HHH_10708;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.junit.Assert;


public class UnexpectedDeleteTwoTestTask extends AbstractEnhancerTestTask {

	private Bar myBar;

	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {Foo.class, Bar.class};
	}

	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( AvailableSettings.SHOW_SQL, Boolean.FALSE.toString() );
		cfg.setProperty( AvailableSettings.USE_SQL_COMMENTS, Boolean.TRUE.toString() );
		cfg.setProperty( AvailableSettings.USE_SQL_COMMENTS, Boolean.TRUE.toString() );
		super.prepare( cfg );

		Session s = getFactory().openSession();
		s.beginTransaction();

		Bar bar = new Bar();
		Foo foo1 = new Foo();
		Foo foo2 = new Foo();
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

		Bar bar = s.get( Bar.class, myBar.id );
		Assert.assertFalse( bar.foos.isEmpty() );

		s.flush();
		s.getTransaction().commit();
		s.close();
	}

	protected void cleanup() {
	}

	@Entity(name = "Bar") static class Bar {

		@Id	@GeneratedValue
		int id;

		@ManyToMany(fetch = FetchType.LAZY, targetEntity = Foo.class)
		Set<Foo> foos = new HashSet<>();
	}

	@Entity(name = "Foo") static class Foo {

		@Id	@GeneratedValue
		int id;
	}

}