/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance.mixed;

import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
public class SubclassTest {

	@Test
	public void testDefault(SessionFactoryScope scope) {
		File doc = new Document( "Enron Stuff To Shred", 1000 );
		Folder folder = new Folder( "Enron" );
		scope.inTransaction(
				session ->
				{
					session.persist( doc );
					session.persist( folder );
				}
		);

		scope.inTransaction(
				session -> {
					CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					CriteriaQuery<File> criteria = criteriaBuilder.createQuery( File.class );
					criteria.from( File.class );
					List<File> result = session.createQuery( criteria ).list();

//					List result = session.createCriteria( File.class ).list();
					assertNotNull( result );
					assertEquals( 2, result.size() );
					File f2 = result.get( 0 );
					checkClassType( f2, doc, folder );
					f2 = result.get( 1 );
					checkClassType( f2, doc, folder );
					session.remove( result.get( 0 ) );
					session.remove( result.get( 1 ) );
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
