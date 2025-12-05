/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.embeddedid;

import java.util.Arrays;

import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7157")
@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "'CONSTANT' is not escaped even though autoQuoteKeywords is enabled")
@EnversTest
@Jpa(annotatedClasses = {Person.class, PersonTuple.class, Constant.class})
public class MapsIdTest {
	private PersonTuple tuple1Ver1 = null;
	private PersonTuple tuple2Ver1 = null;
	private PersonTuple tuple2Ver2 = null;
	private Person personCVer1 = null;
	private Person personCVer2 = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			Person personA = new Person( "Peter" );
			Person personB = new Person( "Mary" );
			em.persist( personA );
			em.persist( personB );
			Constant cons = new Constant( "USD", "US Dollar" );
			em.persist( cons );
			PersonTuple tuple1 = new PersonTuple( true, personA, personB, cons );
			em.persist( tuple1 );

			tuple1Ver1 = new PersonTuple(
					tuple1.isHelloWorld(),
					tuple1.getPersonA(),
					tuple1.getPersonB(),
					tuple1.getConstant()
			);
		} );

		// Revision 2
		scope.inTransaction( em -> {
			Constant cons = em.find( Constant.class, "USD" );
			Person personA = em.createQuery( "select p from Person p where p.name = 'Peter'", Person.class ).getSingleResult();
			Person personC1 = new Person( "Lukasz" );
			em.persist( personC1 );
			PersonTuple tuple2 = new PersonTuple( true, personA, personC1, cons );
			em.persist( tuple2 );

			tuple2Ver1 = new PersonTuple(
					tuple2.isHelloWorld(),
					tuple2.getPersonA(),
					tuple2.getPersonB(),
					tuple2.getConstant()
			);
			personCVer1 = new Person( personC1.getId(), personC1.getName() );
			personCVer1.getPersonBTuples().add( tuple2Ver1 );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			PersonTuple tuple2 = em.createQuery(
				"select pt from PersonTuple pt where pt.personB.name = 'Lukasz'",
				PersonTuple.class
			).getSingleResult();
			tuple2.setHelloWorld( false );
			em.merge( tuple2 );

			tuple2Ver2 = new PersonTuple(
					tuple2.isHelloWorld(),
					tuple2.getPersonA(),
					tuple2.getPersonB(),
					tuple2.getConstant()
			);
		} );

		// Revision 4
		scope.inTransaction( em -> {
			Person personC2 = em.createQuery( "select p from Person p where p.name = 'Lukasz'", Person.class ).getSingleResult();
			personC2.setName( "Robert" );
			em.merge( personC2 );

			personCVer2 = new Person( personC2.getId(), personC2.getName() );
			personCVer2.getPersonBTuples().add( tuple2Ver1 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals(
					Arrays.asList( 1 ),
					auditReader.getRevisions( PersonTuple.class, tuple1Ver1.getPersonTupleId() )
			);
			assertEquals(
					Arrays.asList( 2, 3 ),
					auditReader.getRevisions( PersonTuple.class, tuple2Ver1.getPersonTupleId() )
			);
			assertEquals(
					Arrays.asList( 2, 4 ),
					auditReader.getRevisions( Person.class, personCVer1.getId() )
			);
		} );
	}

	@Test
	public void testHistoryOfTuple1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			PersonTuple tuple = auditReader.find( PersonTuple.class, tuple1Ver1.getPersonTupleId(), 1 );

			assertEquals( tuple1Ver1, tuple );
			assertEquals( tuple1Ver1.isHelloWorld(), tuple.isHelloWorld() );
			assertEquals( tuple1Ver1.getPersonA().getId(), tuple.getPersonA().getId() );
			assertEquals( tuple1Ver1.getPersonB().getId(), tuple.getPersonB().getId() );
		} );
	}

	@Test
	public void testHistoryOfTuple2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			PersonTuple tuple = auditReader.find( PersonTuple.class, tuple2Ver2.getPersonTupleId(), 2 );

			assertEquals( tuple2Ver1, tuple );
			assertEquals( tuple2Ver1.isHelloWorld(), tuple.isHelloWorld() );
			assertEquals( tuple2Ver1.getPersonA().getId(), tuple.getPersonA().getId() );
			assertEquals( tuple2Ver1.getPersonB().getId(), tuple.getPersonB().getId() );

			tuple = auditReader.find( PersonTuple.class, tuple2Ver2.getPersonTupleId(), 3 );

			assertEquals( tuple2Ver2, tuple );
			assertEquals( tuple2Ver2.isHelloWorld(), tuple.isHelloWorld() );
			assertEquals( tuple2Ver2.getPersonA().getId(), tuple.getPersonA().getId() );
			assertEquals( tuple2Ver2.getPersonB().getId(), tuple.getPersonB().getId() );
		} );
	}

	@Test
	public void testHistoryOfPersonC(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			Person person = auditReader.find( Person.class, personCVer1.getId(), 2 );

			assertEquals( personCVer1, person );
			assertEquals( personCVer1.getPersonATuples(), person.getPersonATuples() );
			assertEquals( personCVer1.getPersonBTuples(), person.getPersonBTuples() );

			person = auditReader.find( Person.class, personCVer2.getId(), 4 );

			assertEquals( personCVer2, person );
		} );
	}
}
