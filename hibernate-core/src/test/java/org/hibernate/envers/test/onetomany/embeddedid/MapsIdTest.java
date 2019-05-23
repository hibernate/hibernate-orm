/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetomany.embeddedid;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetomany.embeddedid.Constant;
import org.hibernate.envers.test.support.domains.onetomany.embeddedid.Person;
import org.hibernate.envers.test.support.domains.onetomany.embeddedid.PersonTuple;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7157")
@Disabled("NYI - @MapsId support?")
public class MapsIdTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private PersonTuple tuple1Ver1 = null;
	private PersonTuple tuple2Ver1 = null;
	private PersonTuple tuple2Ver2 = null;
	private Person personCVer1 = null;
	private Person personCVer2 = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class, PersonTuple.class, Constant.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					Person personA = new Person( "Peter" );
					Person personB = new Person( "Mary" );
					entityManager.persist( personA );
					entityManager.persist( personB );
					Constant cons = new Constant( "USD", "US Dollar" );
					entityManager.persist( cons );
					PersonTuple tuple1 = new PersonTuple( true, personA, personB, cons );
					entityManager.persist( tuple1 );

					tuple1Ver1 = new PersonTuple(
							tuple1.isHelloWorld(),
							tuple1.getPersonA(),
							tuple1.getPersonB(),
							tuple1.getConstant()
					);
				},

				// Revision 2
				entityManager -> {
					Constant cons = entityManager.find( Constant.class, tuple1Ver1.getConstant().getId() );
					Person personC1 = new Person( "Lukasz" );
					entityManager.persist( personC1 );
					PersonTuple tuple2 = new PersonTuple( true, tuple1Ver1.getPersonA(), personC1, cons );
					entityManager.persist( tuple2 );

					tuple2Ver1 = new PersonTuple(
							tuple2.isHelloWorld(),
							tuple2.getPersonA(),
							tuple2.getPersonB(),
							tuple2.getConstant()
					);

					personCVer1 = new Person( personC1.getId(), personC1.getName() );
					personCVer1.getPersonBTuples().add( tuple2Ver1 );
				},

				// Revision 3
				entityManager -> {
					PersonTuple tuple2 = entityManager.find( PersonTuple.class, tuple2Ver1.getPersonTupleId() );
					tuple2.setHelloWorld( false );
					entityManager.merge( tuple2 );

					tuple2Ver2 = new PersonTuple(
							tuple2.isHelloWorld(),
							tuple2.getPersonA(),
							tuple2.getPersonB(),
							tuple2.getConstant()
					);
				},

				// Revision 4
				entityManager -> {
					Person personC2 = entityManager.find( Person.class, personCVer1.getId() );
					personC2.setName( "Robert" );
					entityManager.merge( personC2 );

					personCVer2 = new Person( personC2.getId(), personC2.getName() );
					personCVer2.getPersonBTuples().add( tuple2Ver1 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( PersonTuple.class, tuple1Ver1.getPersonTupleId() ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( PersonTuple.class, tuple2Ver1.getPersonTupleId() ), contains( 2, 3 ) );

		assertThat( getAuditReader().getRevisions( Person.class, personCVer1.getId() ), contains( 2, 4 ) );
	}

	@DynamicTest
	public void testHistoryOfTuple1() {
		PersonTuple rev1 = getAuditReader().find( PersonTuple.class, tuple1Ver1.getPersonTupleId(), 1 );
		assertThat( rev1, equalTo( tuple1Ver1 ) );
		assertThat( rev1.isHelloWorld(), equalTo( tuple1Ver1.isHelloWorld() ) );
		assertThat( rev1.getPersonA().getId(), equalTo( tuple1Ver1.getPersonA().getId() ) );
		assertThat( rev1.getPersonB().getId(), equalTo( tuple1Ver1.getPersonB().getId() ) );
	}

	@DynamicTest
	public void testHistoryOfTuple2() {
		PersonTuple rev2 = getAuditReader().find( PersonTuple.class, tuple2Ver2.getPersonTupleId(), 2 );
		assertThat( rev2, equalTo( tuple2Ver1 ) );
		assertThat( rev2.isHelloWorld(), equalTo( tuple2Ver1.isHelloWorld() ) );
		assertThat( rev2.getPersonA().getId(), equalTo( tuple2Ver1.getPersonA().getId() ) );
		assertThat( rev2.getPersonB().getId(), equalTo( tuple2Ver1.getPersonB().getId() ) );

		PersonTuple rev3 = getAuditReader().find( PersonTuple.class, tuple2Ver2.getPersonTupleId(), 3 );
		assertThat( rev3, equalTo( tuple2Ver2 ) );
		assertThat( rev3.isHelloWorld(), equalTo( tuple2Ver2.isHelloWorld() ) );
		assertThat( rev3.getPersonA().getId(), equalTo( tuple2Ver2.getPersonA().getId() ) );
		assertThat( rev3.getPersonB().getId(), equalTo( tuple2Ver2.getPersonB().getId() ) );
	}

	@DynamicTest
	public void testHistoryOfPersonC() {
		Person rev2 = getAuditReader().find( Person.class, personCVer1.getId(), 2 );
		assertThat( rev2, equalTo( personCVer1 ) );
		assertThat( rev2.getPersonATuples(), equalTo( personCVer1.getPersonATuples() ) );
		assertThat( rev2.getPersonBTuples(), equalTo( personCVer1.getPersonBTuples() ) );

		Person rev4 = getAuditReader().find( Person.class, personCVer2.getId(), 4 );
		assertThat( rev4, equalTo( personCVer2 ) );
	}
}