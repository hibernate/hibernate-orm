package org.hibernate.orm.test.hql;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@JiraKey("HHH-18450")
public class SingleParameterSelectionAsObjectArrayResultTest extends BaseCoreFunctionalTestCase {

	@Test
	public void singleParameterAsObjectArray() {
		inSession( s -> {
			assertArrayEquals(
					new Object[] { 1 },
					s.createSelectionQuery( "SELECT ?1", Object[].class )
							.setParameter( 1, 1 )
							.getSingleResult()
			);
			assertArrayEquals(
					new Object[] { 1 },
					s.createSelectionQuery( "SELECT :p1", Object[].class )
							.setParameter( "p1", 1 )
							.getSingleResult()
			);
		} );
	}
}
