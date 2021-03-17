/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.MappedSuperclass;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

@TestForIssue(jiraKey = "")
@RunWith(BytecodeEnhancerRunner.class)
public class LazyCollectionHandlingTest extends BaseCoreFunctionalTestCase {

	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				JafSid.class, UserGroup.class
		};
	}

	@Test
	public void test() {
		doInHibernate( this::sessionFactory, s -> {
			JafSid sid = new JafSid();
			s.save( sid );

			s.flush();
			s.clear();

			this.id = sid.getId();
		});

		doInHibernate( this::sessionFactory, s -> {
			s.get( JafSid.class, this.id );
		} );
	}

	@MappedSuperclass
	public abstract static class DatabaseEntity {
		private int id;

		@Id
		@GeneratedValue
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

	}

	@Entity(name = "JafSid")
	public static class JafSid extends DatabaseEntity {

		private Set<UserGroup> groups = new LinkedHashSet<>();

		@ManyToMany(mappedBy = "members", fetch = FetchType.EAGER)
		public Set<UserGroup> getGroups() {
			return groups;
		}

		public void setGroups(Set<UserGroup> groups) {
			this.groups = groups;
		}
	}

	@Entity(name = "UserGroup")
	public static class UserGroup extends DatabaseEntity {

		private Set<JafSid> members = new LinkedHashSet<>();

		@ManyToMany
		public Set<JafSid> getMembers() {
			return members;
		}

		public void setMembers(Set<JafSid> members) {
			this.members = members;
		}
	}
}
