/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idclass;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

@TestForIssue( jiraKey = "HHH-12996" )
public class NestedIdClassQueryTest extends BaseCoreFunctionalTestCase {

	@Entity(name = "BasicEntity")
	public static class BasicEntity {
		@Id Long key1;
	}

	@Entity(name = "IdClassEntity")
	@IdClass( IdClassEntity.IdClassEntityId.class )
	public static class IdClassEntity {
		@Id @ManyToOne BasicEntity basicEntity;
		@Id Long key2;

		public static class IdClassEntityId implements Serializable {
			Long basicEntity;
			Long key2;
		}
	}

	@Entity(name = "NestedIdClassEntity")
	@IdClass( NestedIdClassEntity.NestedIdClassEntityId.class )
	public static class NestedIdClassEntity {
		@Id @ManyToOne IdClassEntity idClassEntity;
		@Id Long key3;

		public static class NestedIdClassEntityId implements Serializable {
			IdClassEntity.IdClassEntityId idClassEntity;
			Long key3;
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?> [] { BasicEntity.class, IdClassEntity.class, NestedIdClassEntity.class };
	}

	@Test
	@FailureExpected( jiraKey = "HHH-12996")
	public void test() {
		doInHibernate( this::sessionFactory, s -> {
			s.createQuery( "SELECT a.idClassEntity.basicEntity.key1 FROM NestedIdClassEntity a " ).getResultList();
		} );
	}

}
