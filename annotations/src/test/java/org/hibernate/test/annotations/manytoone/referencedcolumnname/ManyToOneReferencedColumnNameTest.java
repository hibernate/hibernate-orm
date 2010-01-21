//$
package org.hibernate.test.annotations.manytoone.referencedcolumnname;

import java.math.BigDecimal;

import org.hibernate.test.annotations.TestCase;
import org.hibernate.Session;

/**
 * @author Emmanuel Bernard
 */
public class ManyToOneReferencedColumnNameTest extends TestCase {
	public void testReoverableExceptionInFkOrdering() throws Exception {
		//SF should not blow up
		Vendor v = new Vendor();
		Item i = new Item();
		ZItemCost ic = new ZItemCost();
		ic.setCost( new BigDecimal(2) );
		ic.setItem( i );
		ic.setVendor( v );
		WarehouseItem wi = new WarehouseItem();
		wi.setDefaultCost( ic );
		wi.setItem( i );
		wi.setVendor( v );
		wi.setQtyInStock( new BigDecimal(2) );
		Session s = openSession(  );
		s.getTransaction().begin();
		s.save( i );
		s.save( v );
		s.save( ic );
		s.save( wi );
		s.flush();
		s.getTransaction().rollback();
		s.close();
		
	}
	
	@Override
	protected boolean runForCurrentDialect() {
		return super.runForCurrentDialect() && getDialect().supportsIdentityColumns();
	}



	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Item.class,
				Vendor.class,
				WarehouseItem.class,
				ZItemCost.class
		};
	}
}
