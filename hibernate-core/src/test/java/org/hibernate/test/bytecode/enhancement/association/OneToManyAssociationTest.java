/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.association;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Luis Barreiro
 */
@RunWith( BytecodeEnhancerRunner.class )
public class OneToManyAssociationTest {

    @Test
    public void test() {
        Customer customer = new Customer();
        Assert.assertTrue( customer.getInventories().isEmpty() );

        CustomerInventory customerInventory = new CustomerInventory();
        customerInventory.setCustomer( customer );

        Assert.assertEquals( 1, customer.getInventories().size() );
        Assert.assertTrue( customer.getInventories().contains( customerInventory ) );

        Customer anotherCustomer = new Customer();
        Assert.assertTrue( anotherCustomer.getInventories().isEmpty() );
        customerInventory.setCustomer( anotherCustomer );

        Assert.assertTrue( customer.getInventories().isEmpty() );
        Assert.assertEquals( 1, anotherCustomer.getInventories().size() );
        Assert.assertSame( customerInventory, anotherCustomer.getInventories().get( 0 ) );

        customer.addInventory( customerInventory );

        Assert.assertSame( customer, customerInventory.getCustomer() );
        Assert.assertTrue( anotherCustomer.getInventories().isEmpty() );
        Assert.assertEquals( 1, customer.getInventories().size() );

        customer.addInventory( new CustomerInventory() );
        Assert.assertEquals( 2, customer.getInventories().size() );

        // Test remove
        customer.removeInventory( customerInventory );
        Assert.assertEquals( 1, customer.getInventories().size() );

        // This happens (and is expected) because there was no snapshot taken before remove
        Assert.assertNotNull( customerInventory.getCustomer() );
    }

    // --- //

    @Entity
    private static class Customer {

        @Id
        Long id;

        String name;

        // HHH-13446 - Type not validated in bi-directional association mapping
        @OneToMany(cascade = CascadeType.ALL, mappedBy = "custId", fetch = FetchType.EAGER)
        List<CustomerInventory> inventoryIdList = new ArrayList<>();

        @OneToMany( mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.EAGER )
        List<CustomerInventory> customerInventories = new ArrayList<>();

        void addInventory(CustomerInventory inventory) {
            List<CustomerInventory> list = customerInventories;
            list.add( inventory );
            customerInventories = list;
        }

        List<CustomerInventory> getInventories() {
            return Collections.unmodifiableList( customerInventories );
        }

        void removeInventory(CustomerInventory inventory) {
            customerInventories.remove( inventory );
        }
    }

    @Entity
    private static class CustomerInventory {

        @Id
        Long id;

        @Id
        Long custId;

        @ManyToOne( cascade = CascadeType.MERGE )
        Customer customer;

        @ManyToOne( cascade = CascadeType.MERGE )
        String vehicle;

        Customer getCustomer() {
            return customer;
        }

        void setCustomer(Customer customer) {
            this.customer = customer;
        }

        void setVehicle(String vehicle) {
            this.vehicle = vehicle;
        }
    }
}
