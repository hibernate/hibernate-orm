/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.bytecode.enhancement;

import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.test.bytecode.enhancement.entity.customer.Address;
import org.hibernate.test.bytecode.enhancement.entity.customer.Customer;
import org.hibernate.test.bytecode.enhancement.entity.customer.CustomerInventory;
import org.hibernate.test.bytecode.enhancement.entity.customer.SupplierComponentPK;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * @author Steve Ebersole
 */
public class CustomerEnhancerTest extends BaseUnitTestCase {

    @Test
    public void testEnhancement() throws Exception {
        testFor(Customer.class);
    }

    private void testFor(Class entityClassToEnhance) throws Exception {
        ClassLoader cl = new ClassLoader() {};

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
        setter.invoke(entityInstance, new Object[] {null});
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
        assertSame( entityInstance, nextGetter.invoke(entityInstance));

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

}
