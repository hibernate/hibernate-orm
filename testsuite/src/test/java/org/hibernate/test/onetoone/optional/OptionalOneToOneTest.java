package org.hibernate.test.onetoone.optional;

import junit.framework.Test;

import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.Session;

/**
 * @author Gavin King
 */
public class OptionalOneToOneTest extends FunctionalTestCase {

	public OptionalOneToOneTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "onetoone/optional/Person.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false");
		cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( OptionalOneToOneTest.class );
	}

	public void testOptionalOneToOneRetrieval() {
		Session s = openSession();
		s.beginTransaction();
		Person me = new Person();
		me.name = "Steve";
		s.save( me );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		me = ( Person ) s.load( Person.class, me.name );
		assertNull( me.address );
		s.delete( me );
		s.getTransaction().commit();
		s.close();
	}
}
