/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ecid;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/ecid/Course.hbm.xml"
)
@SessionFactory
public class EmbeddedCompositeIdTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testMerge(SessionFactoryScope scope) {
		Course uc = new UniversityCourse( "mat2000", "Monash", "second year maths", 0 );
		Course c = new Course( "eng5000", "BHS", "grade 5 english" );
		scope.inTransaction(
				session -> {
					session.persist( uc );
					session.persist( c );
				}
		);

		c.setDescription( "Grade 5 English" );
		uc.setDescription( "Second year mathematics" );

		scope.inTransaction(
				session -> {
					session.merge( c );
					session.merge( uc );
				}
		);

		scope.inTransaction(
				session -> {
					session.remove( c );
					session.remove( uc );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-799")
	public void testMerging(SessionFactoryScope scope) {
		Course course = new Course( "EN-101", "BA", "preparatory english" );
		scope.inTransaction(
				session ->
						session.persist( course )
		);

		String newDesc = "basic preparatory english";
		course.setDescription( newDesc );

		scope.inTransaction(
				session -> {
					Course c = (Course) session.merge( course );
					assertEquals( newDesc, c.getDescription(), "description not merged" );
				}
		);

		scope.inTransaction(
				session -> {
					Course cid = new Course( "EN-101", "BA", null );
					Course c = session.get( Course.class, cid );
					assertEquals( newDesc, c.getDescription(), "description not merged" );
					session.remove( c );
				}
		);
	}

	@Test
	public void testPolymorphism(SessionFactoryScope scope) {
		Course uc = new UniversityCourse( "mat2000", "Monash", "second year maths", 0 );
		Course c = new Course( "eng5000", "BHS", "grade 5 english" );
		scope.inTransaction(
				session -> {
					session.persist( uc );
					session.persist( c );
				}
		);

		scope.inTransaction(
				session -> {
					Course ucid = new Course( "mat2000", "Monash", null );
					Course cid = new Course( "eng5000", "BHS", null );
					Course luc = session.getReference( Course.class, ucid );
					Course lc = session.getReference( Course.class, cid );
					assertFalse( Hibernate.isInitialized( luc ) );
					assertFalse( Hibernate.isInitialized( lc ) );
					assertEquals( UniversityCourse.class, Hibernate.getClass( luc ) );
					assertEquals( Course.class, Hibernate.getClass( lc ) );
					assertSame( ( (HibernateProxy) lc ).getHibernateLazyInitializer().getImplementation(), cid );
					assertEquals( "eng5000", c.getCourseCode() );
					assertEquals( "mat2000", uc.getCourseCode() );
				}
		);

		scope.inTransaction(
				session -> {
					Course ucid = new Course( "mat2000", "Monash", null );
					Course cid = new Course( "eng5000", "BHS", null );
					Course luc = session.get( Course.class, ucid );
					Course lc = session.get( Course.class, cid );
					assertTrue( Hibernate.isInitialized( luc ) );
					assertTrue( Hibernate.isInitialized( lc ) );
					assertEquals( UniversityCourse.class, Hibernate.getClass( luc ) );
					assertEquals( Course.class, Hibernate.getClass( lc ) );
					assertSame( lc, cid );
					assertEquals( "eng5000", c.getCourseCode() );
					assertEquals( "mat2000", uc.getCourseCode() );
				}
		);

		List<Course> courses = scope.fromTransaction(
				session -> {
					List<Course> list = session.createQuery( "from Course order by courseCode" ).list();
					assertTrue( list.get( 0 ) instanceof Course );
					assertTrue( list.get( 1 ) instanceof UniversityCourse );
					assertEquals( "eng5000", list.get( 0 ).getCourseCode() );
					assertEquals( "mat2000", list.get( 1 ).getCourseCode() );
					return list;
				}
		);


		courses.get( 0 ).setDescription( "Grade 5 English" );
		courses.get( 1 ).setDescription( "Second year mathematics" );

		scope.inTransaction(
				session -> {
					session.merge( courses.get( 0 ) );
					session.merge( courses.get( 1 ) );
				}
		);

		scope.inTransaction(
				session ->
						courses.forEach( course -> session.remove( c ) )
		);
	}
}
