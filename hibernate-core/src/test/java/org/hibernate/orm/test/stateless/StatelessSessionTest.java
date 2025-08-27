/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import java.util.Date;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.Transaction;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/stateless/Document.hbm.xml"
)
@SessionFactory
public class StatelessSessionTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testCreateUpdateReadDelete(SessionFactoryScope scope) {
		scope.inStatelessSession(
				statelessSession -> {
					try {
						Transaction tx = statelessSession.beginTransaction();
						Document doc = new Document( "blah blah blah", "Blahs" );
						statelessSession.insert( doc );
						assertNotNull( doc.getName() );
						Date initVersion = doc.getLastModified();
						assertNotNull( initVersion );
						tx.commit();

						tx = statelessSession.beginTransaction();
						doc.setText( "blah blah blah .... blah" );
						statelessSession.update( doc );
						assertNotNull( doc.getLastModified() );
						assertNotSame( doc.getLastModified(), initVersion );
						tx.commit();

						tx = statelessSession.beginTransaction();
						doc.setText( "blah blah blah .... blah blay" );
						statelessSession.update( doc );
						tx.commit();

						Document doc2 = (Document) statelessSession.get( Document.class.getName(), "Blahs" );
						assertEquals( "Blahs", doc2.getName() );
						assertEquals( doc.getText(), doc2.getText() );

						doc2 = (Document) statelessSession.createQuery( "from Document where text is not null" )
								.uniqueResult();
						assertEquals( "Blahs", doc2.getName() );
						assertEquals( doc.getText(), doc2.getText() );

						try (var sr = statelessSession.createQuery( "from Document where text is not null" )
								.scroll( ScrollMode.FORWARD_ONLY )) {
							sr.next();
							doc2 = (Document) sr.get();
						}
						assertEquals( "Blahs", doc2.getName() );
						assertEquals( doc.getText(), doc2.getText() );

						doc2 = (Document) statelessSession.createNativeQuery( "select * from Document" )
								.addEntity( Document.class )
								.uniqueResult();
						assertEquals( "Blahs", doc2.getName() );
						assertEquals( doc.getText(), doc2.getText() );


						CriteriaBuilder criteriaBuilder = statelessSession.getCriteriaBuilder();
						CriteriaQuery<Document> criteria = criteriaBuilder.createQuery( Document.class );
						criteria.from( Document.class );
						doc2 = statelessSession.createQuery( criteria ).uniqueResult();
						assertEquals( "Blahs", doc2.getName() );
						assertEquals( doc.getText(), doc2.getText() );

						criteria = criteriaBuilder.createQuery( Document.class );
						criteria.from( Document.class );

						try (var sr = statelessSession.createQuery( criteria ).scroll( ScrollMode.FORWARD_ONLY )) {
							sr.next();
							doc2 = sr.get();
						}
						assertEquals( "Blahs", doc2.getName() );
						assertEquals( doc.getText(), doc2.getText() );

						tx = statelessSession.beginTransaction();
						statelessSession.delete( doc );
						tx.commit();
					}
					catch (Exception e) {
						if ( statelessSession.getTransaction().isActive() ) {
							statelessSession.getTransaction().rollback();
						}
						throw e;
					}
				} );
	}

	@Test
	public void testGetNull(SessionFactoryScope scope) {
		scope.inStatelessSession(
				statelessSession -> {
					assertNull( statelessSession.get( Document.class, "Blank" ) );
				}
		);
		scope.inStatelessTransaction(
				statelessSession -> {
					assertNull( statelessSession.get( Document.class, "Blank", LockMode.PESSIMISTIC_WRITE ) );
				}
		);
	}

	@Test
	public void testHqlBulk(SessionFactoryScope scope) {
		scope.inStatelessSession(
				statelessSession -> {
					try {
						Transaction tx = statelessSession.beginTransaction();
						Document doc = new Document( "blah blah blah", "Blahs" );
						statelessSession.insert( doc );
						Paper paper = new Paper();
						paper.setColor( "White" );
						statelessSession.insert( paper );
						tx.commit();

						tx = statelessSession.beginTransaction();
						int count = statelessSession.createQuery(
								"update Document set name = :newName where name = :oldName" )
								.setParameter( "newName", "Foos" )
								.setParameter( "oldName", "Blahs" )
								.executeUpdate();
						assertEquals( 1, count, "hql-update on stateless session" );
						count = statelessSession.createQuery( "update Paper set color = :newColor" )
								.setParameter( "newColor", "Goldenrod" )
								.executeUpdate();
						assertEquals( 1, count, "hql-update on stateless session" );
						tx.commit();

						tx = statelessSession.beginTransaction();
						count = statelessSession.createQuery( "delete Document" ).executeUpdate();
						assertEquals( 1, count, "hql-delete on stateless session" );
						count = statelessSession.createQuery( "delete Paper" ).executeUpdate();
						assertEquals( 1, count, "hql-delete on stateless session" );
						tx.commit();
					}
					finally {
						if ( statelessSession.getTransaction().isActive() ) {
							statelessSession.getTransaction().rollback();
						}
					}
				}
		);
	}

	@Test
	public void testInitId(SessionFactoryScope scope) {
		scope.inStatelessSession(
				statelessSession -> {
					try {
						Transaction tx = statelessSession.beginTransaction();
						Paper paper = new Paper();
						paper.setColor( "White" );
						statelessSession.insert( paper );
						assertNotNull( paper.getId() );
						tx.commit();

						tx = statelessSession.beginTransaction();
						statelessSession.delete( statelessSession.get( Paper.class, paper.getId() ) );
						tx.commit();
					}
					finally {
						if ( statelessSession.getTransaction().isActive() ) {
							statelessSession.getTransaction().rollback();
						}
					}
				}
		);
	}

	@Test
	public void testRefresh(SessionFactoryScope scope) {
		Paper paper = new Paper();
		scope.inStatelessTransaction(
				statelessSession -> {
					paper.setColor( "whtie" );
					statelessSession.insert( paper );
				}
		);

		scope.inStatelessTransaction(
				statelessSession -> {
					Paper p2 = statelessSession.get( Paper.class, paper.getId() );
					p2.setColor( "White" );
					statelessSession.update( p2 );
				}
		);

		scope.inStatelessTransaction(
				statelessSession -> {
					assertEquals( "whtie", paper.getColor() );
					statelessSession.refresh( paper );
					assertEquals( "White", paper.getColor() );
					statelessSession.delete( paper );
				}
		);
	}
}
