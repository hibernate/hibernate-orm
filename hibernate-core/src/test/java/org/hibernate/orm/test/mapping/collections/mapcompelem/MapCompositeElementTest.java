/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.mapcompelem;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


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
			assertEquals( prod.getParts().size(), 1 );
			prod.getParts().put("Top", new Part("top", "The brand new top part"));
		} );

		scope.inTransaction( (s) -> {
			final Product prod = s.get(Product.class, "Some Thing");
			assertEquals( prod.getParts().size(), 1 );
			assertEquals( ( (Part) prod.getParts().get("Top") ).getDescription(), "The brand new top part" );
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

			List list = s.createQuery("select new Part( part.name, part.description ) from Product prod join prod.parts part order by part.name desc").list();
			assertEquals( list.size(), 2 );
			assertTrue( list.get(0) instanceof Part );
			assertTrue( list.get(1) instanceof Part );
			Part part = (Part) list.get(0);
			assertEquals( part.getName(), "top" );
			assertEquals( part.getDescription(), "The top part" );

			list = s.createQuery("select new Part( part.name, part.description ) from Product prod join prod.parts part where index(part) = 'Top'").list();
			assertEquals( list.size(), 1 );
			assertTrue( list.get(0) instanceof Part );
			part = (Part) list.get(0);
			assertEquals( part.getName(), "top" );
			assertEquals( part.getDescription(), "The top part" );

			list = s.createQuery("from Product p where 'Top' in indices(p.parts)").list();
			assertEquals( list.size(), 1 );
			assertSame( list.get(0), prod );

			list = s.createQuery("select i from Item i join i.product p where 'Top' in indices(p.parts)").list();
			assertEquals( list.size(), 1 );
			assertSame( list.get(0), item );

			list = s.createQuery("from Item i where 'Top' in indices(i.product.parts)").list();
			assertEquals( list.size(), 1 );
			assertSame( list.get(0), item );

			s.remove(item);
			s.remove(prod);
		} );
	}

}
