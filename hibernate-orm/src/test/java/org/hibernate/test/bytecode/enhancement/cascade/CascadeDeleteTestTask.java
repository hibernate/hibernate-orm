/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.cascade;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;

/**
 * @author Luis Barreiro
 */
public class CascadeDeleteTestTask extends AbstractEnhancerTestTask {


	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {Parent.class, Child.class};
	}

	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		super.prepare( cfg );

		// Create a Parent with one Child
		Session s = getFactory().openSession();
		s.beginTransaction();

		Parent p = new Parent();
		p.setName("PARENT");
		p.setLazy("LAZY");

		Child child = p.makeChild();
		s.persist(p);

		s.getTransaction().commit();
		s.close();
	}

	public void execute() {
		// Delete the Parent
		Session s = getFactory().openSession();
		s.beginTransaction();
		Parent loadedParent = (Parent) s.createQuery( "SELECT p FROM Parent p WHERE name=:name" )
				.setParameter( "name", "PARENT" )
				.uniqueResult();

		s.delete( loadedParent );
		s.getTransaction().commit();
		s.close();
		// If the lazy relation is not fetch on cascade there is a constraint violation on commit
	}

	protected void cleanup() {
	}


}
