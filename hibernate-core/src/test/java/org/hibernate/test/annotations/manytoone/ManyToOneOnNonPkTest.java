//$Id$
package org.hibernate.test.annotations.manytoone;

import java.util.Date;

import org.hibernate.test.annotations.TestCase;
import org.hibernate.Session;


/**
 * FIXME test for ANN-548
 * @author Emmanuel Bernard
 */
public class ManyToOneOnNonPkTest extends TestCase {

	public void testNonPkPartOfPk() throws Exception {
//		Session s = openSession( );
//		s.getTransaction().begin();
//
//		LotzPK pk = new LotzPK();
//		pk.setId( 1 );
//		pk.setLocCode( "fr" );
//		Lotz lot = new Lotz();
//		lot.setLocation( "France" );
//		lot.setName( "Chez Dede" );
//		lot.setLotPK( pk );
//		Carz car = new Carz();
//		car.setId( 1 );
//		car.setLot( lot );
//		car.setMake( "Citroen" );
//		car.setManufactured( new Date() );
//		car.setModel( "C5" );
//		s.persist( lot );
//		s.persist( car );
//
//		s.flush();
//		s.clear();
//		s.clear();
//
//		car = (Carz) s.createQuery( "from Carz car left join fetch car.lot").uniqueResult();
//		assertNotNull( car.getLot() );
//
//		s.getTransaction().commit();
//		s.close();
//
	}

	protected Class[] getAnnotatedClasses() {
		return new Class[] {
			//Carz.class,
			//Lotz.class
		};
	}
}
