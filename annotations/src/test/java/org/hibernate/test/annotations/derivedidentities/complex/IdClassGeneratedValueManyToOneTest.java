package org.hibernate.test.annotations.derivedidentities.complex;

import java.math.BigDecimal;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;

import org.hibernate.junit.FailureExpected;
/**
 * A test.
 * 
 * @author <a href="mailto:stale.pedersen@jboss.org">Stale W. Pedersen</a>
 * @version $Revision: 1.1 $
 */
public class IdClassGeneratedValueManyToOneTest extends TestCase
{
   @FailureExpected(jiraKey="HHH-4848")
   public void testComplexIdClass()
   {
      Logger.getLogger("org.hibernate").setLevel(Level.TRACE);
      Session s = openSession();
      Transaction tx = s.beginTransaction();
      
      Customer c1 = new Customer("foo", "bar", "contact1", "100", new BigDecimal(1000),new BigDecimal(1000), new BigDecimal(1000));

      s.persist(c1);
      Item boat = new Item();
      boat.setId("1");
      boat.setName("cruiser");
      boat.setPrice(new BigDecimal(500));
      boat.setDescription("a boat");
      boat.setCategory(42);
      
      s.persist(boat);
      s.flush();
      s.clear();
      
      c1.addInventory(boat, 10, new BigDecimal(5000));
      s.merge(c1);
      s.flush();
      s.clear();

      Customer c2 = (Customer) s.createQuery( "select c from Customer c" ).uniqueResult();

      List<CustomerInventory> inventory = c2.getInventories();
      
      assertEquals(1, inventory.size());
      assertEquals(10, inventory.get(0).getQuantity());
      
      tx.rollback();
      s.close();
      
      assertTrue(true);
   }
   
   protected Class[] getAnnotatedClasses() 
   {
      return new Class[] {
            Customer.class, 
            CustomerInventory.class,
            CustomerInventoryPK.class,
            Item.class
            
      };
   }
}
