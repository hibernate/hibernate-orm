/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.fromcore;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Hardy Ferentschik
 */
public class CoreTest extends CompilationTest {
	@Test
	@WithClasses({MapEntity.class, MapEntityLocal.class, Order.class, LineItem.class, CreditCard.class, Customer.class,
	PersonId.class, Phone.class, Person.class, Product.class, BaseEmbeddedEntity.class, VersionedEntity.class, ShelfLife.class,
	SomeMappedSuperclass.class, SomeMappedSuperclassSubclass.class, Article.class, Alias.class, Address.class, Info.class,
	Country.class, Thing.class, ThingWithQuantity.class, Translation.class, Entity1.class, Entity2.class, Entity3.class})
	public void testGeneratedAnnotationNotGenerated() throws Exception {
		for (Class<?> c : getClass().getMethod("testGeneratedAnnotationNotGenerated").getAnnotation(WithClasses.class).value()) {
			assertMetamodelClassGeneratedFor(c);
		}
	}
}
