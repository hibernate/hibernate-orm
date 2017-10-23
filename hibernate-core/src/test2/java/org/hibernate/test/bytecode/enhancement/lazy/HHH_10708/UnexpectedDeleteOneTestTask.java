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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;

public class UnexpectedDeleteOneTestTask extends AbstractEnhancerTestTask {

	private int fooId;

	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {Foo.class, Bar.class};
	}

	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( AvailableSettings.SHOW_SQL, Boolean.FALSE.toString() );
		cfg.setProperty( AvailableSettings.USE_SQL_COMMENTS, Boolean.TRUE.toString() );
		super.prepare( cfg );

		Session s = getFactory().openSession();
		s.beginTransaction();

		Bar bar1 = new Bar();
		Bar bar2 = new Bar();
		Foo foo = new Foo();
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

		Foo foo = s.get( Foo.class, fooId );

		// accessing the collection results in an exception
		foo.bars.size();

		s.flush();
		s.getTransaction().commit();
		s.close();
	}

	protected void cleanup() {
	}

	@Entity(name = "Bar") static class Bar {
		static final String FOO = "foo";

		@Id	@GeneratedValue
		int id;

		@ManyToOne @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		Foo foo;
	}

	@Entity(name = "Foo") static class Foo {

		@Id	@GeneratedValue
		int id;

		@OneToMany(orphanRemoval = true, mappedBy = Bar.FOO, targetEntity = Bar.class)
		@Cascade(CascadeType.ALL)
		Set<Bar> bars = new HashSet<>();
	}

}