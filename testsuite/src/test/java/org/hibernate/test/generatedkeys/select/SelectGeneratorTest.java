package org.hibernate.test.generatedkeys.select;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.dialect.DataDirectOracle9Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.Oracle9Dialect;
import org.hibernate.junit.functional.DatabaseSpecificFunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Steve Ebersole
 */
public class SelectGeneratorTest extends DatabaseSpecificFunctionalTestCase {
	public SelectGeneratorTest(String x) {
		super( x );
	}

	// TODO : need to determine appropriate physical generation strategies for select-generator testing on other databases...

	public String[] getMappings() {
		return new String[] { "generatedkeys/select/MyEntity.hbm.xml" };
	}

	public boolean appliesTo(Dialect dialect) {
		return ( dialect instanceof Oracle9Dialect );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( SelectGeneratorTest.class );
	}

	public void testJDBC3GetGeneratedKeysSupportOnOracle() {
		if ( getDialect() instanceof DataDirectOracle9Dialect ) {
			reportSkip( "DataDirect drivers known to not support JDBC3 getGeneratedKeys for Oracle", "oracle getGeneratedKeys support" );
			return;
		}
		Session session = openSession();
		session.beginTransaction();

		MyEntity e = new MyEntity( "entity-1" );
		session.save( e );

		// this insert should happen immediately!
		assertEquals( "id not generated through forced insertion", new Long(1), e.getId() );

		session.delete( e );
		session.getTransaction().commit();
		session.close();
	}
}
