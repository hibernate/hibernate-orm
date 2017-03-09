/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetomany;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Stephen Fikes
 * @author Gail Badner
 */
public class OneToManyFKNotInTargetClassTable extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	@TestForIssue(jiraKey = "HHH-10500")
	public void testJoinColumnInSuperclass() {
		Goal goal = new Goal( "goal" );
		Task task1 = new Task( "task1" );
		Task task2 = new Task( "task2" );
		OtherTask otherTask = new OtherTask( "other task");
		Session s = openSession();
		s.beginTransaction();
		s.persist( goal );
		s.persist( task1 );
		s.persist( task2 );
		s.persist( otherTask );
		goal.getTasks().add( task1 );
		goal.getTasks().add( task2 );
		goal.getOtherTasks().add( otherTask );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		goal = s.get( Goal.class, goal.getName() );
		assertEquals( 2, goal.getTasks().size() );
		assertEquals( 1, goal.getOtherTasks().size() );
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				TaskBase.class,
				Task.class,
				OtherTask.class,
				Goal.class
		};
	}
}