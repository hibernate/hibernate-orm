/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.bytecode.enhancement.entity.customer.Address;
import org.hibernate.test.bytecode.enhancement.entity.customer.Customer;
import org.hibernate.test.bytecode.enhancement.entity.customer.CustomerInventory;
import org.hibernate.test.bytecode.enhancement.entity.customer.Group;
import org.hibernate.test.bytecode.enhancement.entity.customer.SupplierComponentPK;
import org.hibernate.test.bytecode.enhancement.entity.customer.User;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class CustomerEnhancerTest extends BaseUnitTestCase {

    @Test
    public void testEnhancement() throws Exception {
        testFor(Customer.class);
    }

    private void testFor(Class entityClassToEnhance) throws Exception {
        ClassLoader cl = new ClassLoader() {
        };

        // just for debugging
        Class<?> addressClass = EnhancerTestUtils.enhanceAndDecompile(Address.class, cl);
        Class<?> customerInventoryClass = EnhancerTestUtils.enhanceAndDecompile(CustomerInventory.class, cl);
        Class<?> supplierComponentPKCtClass = EnhancerTestUtils.enhanceAndDecompile(SupplierComponentPK.class, cl);

        Class<?> entityClass = EnhancerTestUtils.enhanceAndDecompile(entityClassToEnhance, cl);
        Object entityInstance = entityClass.newInstance();
        assertTyping(ManagedEntity.class, entityInstance);

        // call the new methods
        Method setter = entityClass.getMethod(EnhancerConstants.ENTITY_ENTRY_SETTER_NAME, EntityEntry.class);
        Method getter = entityClass.getMethod(EnhancerConstants.ENTITY_ENTRY_GETTER_NAME);
        assertNull(getter.invoke(entityInstance));
        setter.invoke(entityInstance, EnhancerTestUtils.makeEntityEntry());
        assertNotNull(getter.invoke(entityInstance));
        setter.invoke(entityInstance, new Object[]{null});
        assertNull(getter.invoke(entityInstance));

        Method entityInstanceGetter = entityClass.getMethod(EnhancerConstants.ENTITY_INSTANCE_GETTER_NAME);
        assertSame(entityInstance, entityInstanceGetter.invoke(entityInstance));

        Method previousGetter = entityClass.getMethod(EnhancerConstants.PREVIOUS_GETTER_NAME);
        Method previousSetter = entityClass.getMethod(EnhancerConstants.PREVIOUS_SETTER_NAME, ManagedEntity.class);
        previousSetter.invoke(entityInstance, entityInstance);
        assertSame(entityInstance, previousGetter.invoke(entityInstance));

        Method nextGetter = entityClass.getMethod(EnhancerConstants.PREVIOUS_GETTER_NAME);
        Method nextSetter = entityClass.getMethod(EnhancerConstants.PREVIOUS_SETTER_NAME, ManagedEntity.class);
        nextSetter.invoke(entityInstance, entityInstance);
        assertSame(entityInstance, nextGetter.invoke(entityInstance));

        // add an attribute interceptor...
        assertNull(entityClass.getMethod(EnhancerConstants.INTERCEPTOR_GETTER_NAME).invoke(entityInstance));
        entityClass.getMethod("getId").invoke(entityInstance);

        Method interceptorSetter = entityClass.getMethod(EnhancerConstants.INTERCEPTOR_SETTER_NAME, PersistentAttributeInterceptor.class);
        interceptorSetter.invoke(entityInstance, new EnhancerTestUtils.LocalPersistentAttributeInterceptor());
        assertNotNull(entityClass.getMethod(EnhancerConstants.INTERCEPTOR_GETTER_NAME).invoke(entityInstance));

        // dirty checking is unfortunately just printlns for now... just verify the test output
        entityClass.getMethod("getId").invoke(entityInstance);
        entityClass.getMethod("setId", Integer.class).invoke(entityInstance, entityClass.getMethod("getId").invoke(entityInstance));
        entityClass.getMethod("setId", Integer.class).invoke(entityInstance, 1);
        EnhancerTestUtils.checkDirtyTracking(entityInstance, "id");

        entityClass.getMethod("setFirstName", String.class).invoke(entityInstance, "Erik");
        entityClass.getMethod("setLastName", String.class).invoke(entityInstance, "Mykland");

        EnhancerTestUtils.checkDirtyTracking(entityInstance, "id", "firstName", "lastName");
        EnhancerTestUtils.clearDirtyTracking(entityInstance);

        // testing composite object
        Object address = addressClass.newInstance();

        entityClass.getMethod("setAddress", addressClass).invoke(entityInstance, address);
        addressClass.getMethod("setCity", String.class).invoke(address, "Arendal");
        EnhancerTestUtils.checkDirtyTracking(entityInstance, "address", "address.city");
        EnhancerTestUtils.clearDirtyTracking(entityInstance);

        //make sure that new composite instances are cleared
        Object address2 = addressClass.newInstance();
        entityClass.getMethod("setAddress", addressClass).invoke(entityInstance, address2);
        addressClass.getMethod("setStreet1", String.class).invoke(address, "Heggedalveien");
        EnhancerTestUtils.checkDirtyTracking(entityInstance, "address");
    }


    @Test
    public void testBiDirectionalAssociationManagement() throws Exception {
        ClassLoader cl = new ClassLoader() {
        };

        Class<?> userClass = EnhancerTestUtils.enhanceAndDecompile(User.class, cl);
        Class<?> groupClass = EnhancerTestUtils.enhanceAndDecompile(Group.class, cl);
        Class<?> customerClass = EnhancerTestUtils.enhanceAndDecompile(Customer.class, cl);
        Class<?> customerInventoryClass = EnhancerTestUtils.enhanceAndDecompile(CustomerInventory.class, cl);

        Object userInstance = userClass.newInstance();
        assertTyping(ManagedEntity.class, userInstance);

        Object groupInstance = groupClass.newInstance();
        assertTyping(ManagedEntity.class, groupInstance);

        Object customerInstance = customerClass.newInstance();
        assertTyping(ManagedEntity.class, customerInstance);

        Object customerInventoryInstance = customerInventoryClass.newInstance();
        assertTyping(ManagedEntity.class, customerInventoryInstance);

        Method interceptorSetter = userClass.getMethod(EnhancerConstants.INTERCEPTOR_SETTER_NAME, PersistentAttributeInterceptor.class);
        interceptorSetter.invoke(userInstance, new EnhancerTestUtils.LocalPersistentAttributeInterceptor());

        /* --- @OneToOne */

        userClass.getMethod("setLogin", String.class).invoke(userInstance, UUID.randomUUID().toString());

        customerClass.getMethod("setUser", userClass).invoke(customerInstance, userInstance);
        assertEquals(customerInstance, userClass.getMethod("getCustomer").invoke(userInstance));

        // check dirty tracking is set automatically with bi-directional association management
        EnhancerTestUtils.checkDirtyTracking(userInstance, "login", "customer");

        Object anotherUser = userClass.newInstance();
        userClass.getMethod("setLogin", String.class).invoke(anotherUser, UUID.randomUUID().toString());

        customerClass.getMethod("setUser", userClass).invoke(customerInstance, anotherUser);
        assertEquals(null, userClass.getMethod("getCustomer").invoke(userInstance));
        assertEquals(customerInstance, userClass.getMethod("getCustomer").invoke(anotherUser));

        userClass.getMethod("setCustomer", customerClass).invoke(userInstance, customerClass.newInstance());
        assertEquals(userInstance, customerClass.getMethod("getUser").invoke(userClass.getMethod("getCustomer").invoke(userInstance)));

        /* --- @OneToMany @ManyToOne */

        assertTrue(((Collection<?>) customerClass.getMethod("getInventories").invoke(customerInstance)).isEmpty());
        customerInventoryClass.getMethod("setCustomer", customerClass).invoke(customerInventoryInstance, customerInstance);

        Collection<?> inventories = (Collection < ?>) customerClass.getMethod("getInventories").invoke(customerInstance);
        assertTrue(inventories.size() == 1);
        assertTrue(inventories.contains(customerInventoryInstance));

        Object anotherCustomer = customerClass.newInstance();
        customerInventoryClass.getMethod("setCustomer", customerClass).invoke(customerInventoryInstance, anotherCustomer);
        assertTrue(((Collection<?>) customerClass.getMethod("getInventories").invoke(customerInstance)).isEmpty());

        customerClass.getMethod("addInventory", customerInventoryClass).invoke(customerInstance, customerInventoryInstance);
        assertTrue(customerInventoryClass.getMethod("getCustomer").invoke(customerInventoryInstance) == customerInstance);

        inventories = (Collection < ?>) customerClass.getMethod("getInventories").invoke(customerInstance);
        assertTrue(inventories.size() == 1);

        customerClass.getMethod("addInventory", customerInventoryClass).invoke(customerInstance, customerInventoryClass.newInstance());
        assertTrue(((Collection<?>) customerClass.getMethod("getInventories").invoke(customerInstance)).size() == 2);

        /* --- @ManyToMany */

        Object anotherGroup = groupClass.newInstance();
        userClass.getMethod("addGroup", groupClass).invoke(userInstance, groupInstance);
        userClass.getMethod("addGroup", groupClass).invoke(userInstance, anotherGroup);
        userClass.getMethod("addGroup", groupClass).invoke(anotherUser, groupInstance);

        assertTrue(((Collection<?>) groupClass.getMethod("getUsers").invoke(groupInstance)).size() == 2);

        groupClass.getMethod("setUsers", Set.class).invoke(groupInstance, new HashSet());
        assertTrue(((Collection<?>) userClass.getMethod("getGroups").invoke(userInstance)).size() == 1);

    }

}
