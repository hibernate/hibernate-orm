/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.collections.norevision;

import java.util.List;
import java.util.Map;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.collections.norevision.Child;
import org.hibernate.envers.test.support.domains.collections.norevision.House;
import org.hibernate.envers.test.support.domains.collections.norevision.Name;
import org.hibernate.envers.test.support.domains.collections.norevision.Parent;
import org.hibernate.envers.test.support.domains.collections.norevision.Person;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public abstract class AbstractCollectionChangeTest extends EnversSessionFactoryBasedFunctionalTest {
	private Person person;
	private Name name1;
	private Name name2;
	private Parent parent;
	private House house;

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.REVISION_ON_COLLECTION_CHANGE, getCollectionChangeValue() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class, Name.class, Parent.class, Child.class, House.class};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		sessionFactoryScope().inTransactions(
				// Rev 1
				session -> {
					person = new Person();
					name1 = new Name();
					name1.setName( "name1" );
					person.getNames().add( name1 );

					session.saveOrUpdate( person );
				},

				// Rev 2
				session -> {
					name1.setName( "Changed name" );

					session.saveOrUpdate( person );
				},

				// Rev 3
				session -> {
					name2 = new Name();
					name2.setName( "name2" );
					person.getNames().add( name2 );				},

				// Rev 4
				session -> {
					Child child1 = new Child();
					parent = new Parent();

					parent.setName( "P1" );
					child1.setParent( parent );
					parent.getChildren().add( child1 );

					session.saveOrUpdate( child1 );
					session.saveOrUpdate( parent );
				},

				// Rev 5
				session -> {
					Child child2 = new Child();
					parent.getChildren().add( child2 );
					child2.setParent( parent );
					session.saveOrUpdate( child2 );
					session.saveOrUpdate( parent );
				},

				// Rev 6
				session -> {
					house = new House();
					house.getColors().add( "Red" );
					session.saveOrUpdate( house );
				},

				// Rev 7
				session -> {
					house.getColors().add( "Blue" );
					session.saveOrUpdate( house );
				}
		);
	}

	@DynamicTest
	public void testPersonRevisionCount() {
		assertThat( getAuditReader().getRevisions( Person.class, person.getId() ), equalTo( getExpectedPersonRevisions() ) );
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-10201")
	public void testParentRevisionCount() {
		assertThat( getAuditReader().getRevisions( Parent.class, parent.getId() ), equalTo( getExpectedParentRevisions() ) );
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-10201")
	public void testHouseRevisionCount() {
		assertThat( getAuditReader().getRevisions( House.class, house.getId() ), equalTo( getExpectedHouseRevisions() ) );
	}

	protected abstract String getCollectionChangeValue();

	protected abstract List<Integer> getExpectedPersonRevisions();

	protected abstract List<Integer> getExpectedParentRevisions();

	protected abstract List<Integer> getExpectedHouseRevisions();
}
