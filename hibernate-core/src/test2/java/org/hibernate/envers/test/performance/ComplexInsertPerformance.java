/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.performance;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.performance.complex.ChildEntity1;
import org.hibernate.envers.test.performance.complex.ChildEntity2;
import org.hibernate.envers.test.performance.complex.RootEntity;

import org.junit.Ignore;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Ignore
public class ComplexInsertPerformance extends AbstractPerformanceTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {RootEntity.class, ChildEntity1.class, ChildEntity2.class};
	}

	private final static int NUMBER_INSERTS = 1000;

	private long idCounter = 0;

	private ChildEntity2 createChildEntity2() {
		ChildEntity2 ce = new ChildEntity2();
		ce.setId( idCounter++ );
		ce.setNumber( 12345678 );
		ce.setData( "some data, not really meaningful" );
		ce.setStrings( new HashSet<String>() );
		ce.getStrings().add( "aaa" );
		ce.getStrings().add( "bbb" );
		ce.getStrings().add( "ccc" );

		return ce;
	}

	private ChildEntity1 createChildEntity1() {
		ChildEntity1 ce = new ChildEntity1();
		ce.setId( idCounter++ );
		ce.setData1( "xxx" );
		ce.setData2( "yyy" );
		ce.setChild1( createChildEntity2() );
		ce.setChild2( createChildEntity2() );

		return ce;
	}

	protected void doTest() {
		for ( int i = 0; i < NUMBER_INSERTS; i++ ) {
			newEntityManager();
			EntityManager entityManager = getEntityManager();

			entityManager.getTransaction().begin();

			RootEntity re = new RootEntity();
			re.setId( idCounter++ );
			re.setData1( "data1" );
			re.setData2( "data2" );
			re.setDate1( new Date() );
			re.setNumber1( 123 );
			re.setNumber2( 456 );
			re.setChild1( createChildEntity1() );
			re.setChild2( createChildEntity1() );
			re.setChild3( createChildEntity1() );

			start();
			entityManager.persist( re );
			entityManager.getTransaction().commit();
			stop();
		}
	}

	public static void main(String[] args) throws IOException {
		ComplexInsertPerformance insertsPerformance = new ComplexInsertPerformance();
		insertsPerformance.test( 3 );
	}
}
