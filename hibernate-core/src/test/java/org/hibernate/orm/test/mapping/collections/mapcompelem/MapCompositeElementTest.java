/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.mapcompelem;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;


/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/mapping/collections/mapcompelem/ProductPart.hbm.xml"
)
@SessionFactory
public class MapCompositeElementTest {

	@SuppressWarnings( {"unchecked"})
	@Test
	public void testMapCompositeElementWithFormula(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			Part top = new Part("top", "The top part");
			Part bottom = new Part("bottom", "The bottom part");
			Product prod = new Product("Some Thing");
			prod.getParts().put("Top", top);
			prod.getParts().put("Bottom", bottom);
			session.persist(prod);
		} );

		scope.inTransaction( (session) -> {
			final Product prod = session.get(Product.class, "Some Thing");
			prod.getParts().remove("Bottom");
		} );

		scope.inTransaction( (session) -> {
			final Product prod = session.get(Product.class, "Some Thing");
			assertEquals( 1, prod.getParts().size() );
			prod.getParts().put("Top", new Part("top", "The brand new top part"));
		} );

		scope.inTransaction( (s) -> {
			final Product prod = s.get(Product.class, "Some Thing");
			assertEquals( 1, prod.getParts().size() );
			assertEquals( "The brand new top part", ( (Part) prod.getParts().get("Top") ).getDescription() );
			s.remove(prod);
		} );
	}

	@SuppressWarnings( {"unchecked"})
	@Test
	public void testQueryMapCompositeElement(SessionFactoryScope scope) {
		scope.inTransaction( (s) -> {
			Part top = new Part("top", "The top part");
			Part bottom = new Part("bottom", "The bottom part");
			Product prod = new Product("Some Thing");
			prod.getParts().put("Top", top);
			prod.getParts().put("Bottom", bottom);
			s.persist(prod);

			Item item = new Item("123456", prod);
			s.persist(item);

			var parts = s.createQuery( Part.class,
					"select new Part( part.name, part.description ) from Product prod join prod.parts part order by part.name desc")
					.list();
			assertEquals( 2, parts.size() );
			assertInstanceOf( Part.class, parts.get( 0 ) );
			assertInstanceOf( Part.class, parts.get( 1 ) );
			Part part = parts.get(0);
			assertEquals( "top", part.getName() );
			assertEquals( "The top part", part.getDescription() );

			parts = s.createQuery( Part.class, "select new Part( part.name, part.description ) from Product prod join prod.parts part where index(part) = 'Top'").list();
			assertEquals( 1, parts.size() );
			assertInstanceOf( Part.class, parts.get( 0 ) );
			part = parts.get(0);
			assertEquals( "top", part.getName() );
			assertEquals( "The top part", part.getDescription() );

			var products = s.createQuery( Product.class,"from Product p where 'Top' in indices(p.parts)").list();
			assertEquals( 1, products.size() );
			assertSame( products.get(0), prod );

			var items = s.createQuery(Item.class, "select i from Item i join i.product p where 'Top' in indices(p.parts)").list();
			assertEquals( 1, items.size() );
			assertSame( items.get(0), item );

			items = s.createQuery(Item.class, "from Item i where 'Top' in indices(i.product.parts)").list();
			assertEquals( 1, items.size() );
			assertSame( items.get(0), item );

			s.remove(item);
			s.remove(prod);
		} );
	}

}
