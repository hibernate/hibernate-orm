/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance.union;

import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				File.class,
				Folder.class,
				Document.class,
				SymbolicLink.class
		}
)
@SessionFactory
public class SubclassTest  {
	@Test
	public void testDefault(SessionFactoryScope scope) {
		File doc = new Document( "Enron Stuff To Shred", 1000 );
		Folder folder = new Folder( "Enron" );
		scope.inTransaction(
				s -> {
					s.persist( doc );
					s.persist( folder );
				}
		);

		scope.inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<File> criteria = criteriaBuilder.createQuery( File.class );
					criteria.from( File.class );
					List<File> result = s.createQuery( criteria ).list();
//					List result = s.createCriteria( File.class ).list();
					assertNotNull( result );
					assertEquals( 2, result.size() );
					File f2 = result.get( 0 );
					checkClassType( f2, doc, folder );
					f2 = result.get( 1 );
					checkClassType( f2, doc, folder );
				}
		);
	}

	private void checkClassType(File fruitToTest, File f, Folder a) {
		if ( fruitToTest.getName().equals( f.getName() ) ) {
			assertFalse( fruitToTest instanceof Folder );
		}
		else if ( fruitToTest.getName().equals( a.getName() ) ) {
			assertTrue( fruitToTest instanceof Folder );
		}
		else {
			fail( "Result does not contains the previously inserted elements" );
		}
	}

}
