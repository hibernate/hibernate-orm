/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.envers.integration.multiplerelations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Query;

import org.hibernate.Session;
import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.type.StandardBasicTypes;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-7681")
public class GroupMemberTest extends BaseEnversJPAFunctionalTestCase {
	private Integer uniqueGroupId;
	private Integer groupMemberId;

    @Override
    protected Class<?>[] getAnnotatedClasses() {
    	return new Class[] { GroupMember.class, MultiGroup.class, UniqueGroup.class };
    }

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final UniqueGroup uniqueGroup = new UniqueGroup();
			final GroupMember groupMember = new GroupMember();
			uniqueGroup.addMember( groupMember );
			entityManager.persist( uniqueGroup );
			entityManager.persist( groupMember );
			uniqueGroupId = uniqueGroup.getId();
			groupMemberId = groupMember.getId();
		} );
		// Revision 2
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final GroupMember groupMember = entityManager.find( GroupMember.class, groupMemberId );
			final MultiGroup multiGroup = new MultiGroup();
			groupMember.addMultiGroup( multiGroup );
			entityManager.persist( multiGroup );
		} );
	}

	@Test
	public void testUniqueGroupFound() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final GroupMember groupMember = entityManager.find( GroupMember.class, groupMemberId );
			assertNotNull( groupMember );
			assertNotNull( groupMember.getUniqueGroup() );
		} );
	}

	@Test
	public void testUniqueGroupFromAuditHistory() {
		assertEquals( uniqueGroupId, getCurrentAuditUniqueGroupId() );
	}

	private Integer getCurrentAuditUniqueGroupId() {
		return TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final Session session = entityManager.unwrap( Session.class );
			final Query query = session.createNativeQuery(
							"SELECT unique_group_id FROM GroupMember_AUD ORDER BY REV DESC"
					)
					.addScalar( "unique_group_id", StandardBasicTypes.INTEGER )
					.setMaxResults( 1 );
			final Object result = query.getSingleResult();
			assertNotNull( result );
			return (Integer) result;
		} );
	}

	@Entity(name = "GroupMember")
	@Audited
	public static class GroupMember {
		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne
		@JoinColumn(name = "unique_group_id", insertable = false, updatable = false)
		private UniqueGroup uniqueGroup;

		@ManyToMany(mappedBy = "members")
		private List<MultiGroup> multiGroups = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public UniqueGroup getUniqueGroup() {
			return uniqueGroup;
		}

		public void setUniqueGroup(UniqueGroup uniqueGroup) {
			this.uniqueGroup = uniqueGroup;
		}

		public List<MultiGroup> getMultiGroups() {
			return multiGroups;
		}

		public void setMultiGroups(List<MultiGroup> multiGroups) {
			this.multiGroups = multiGroups;
		}

		public void addMultiGroup(MultiGroup multiGroup) {
			this.multiGroups.add( multiGroup );
			multiGroup.addMember( this );
		}
	}

	@Entity(name = "UniqueGroup")
	@Audited
	public static class UniqueGroup {
		@Id
		@GeneratedValue
		private Integer id;

		@OneToMany
		@JoinColumn(name = "unique_group_id")
		@AuditMappedBy(mappedBy = "uniqueGroup")
		private Set<GroupMember> members = new HashSet<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Set<GroupMember> getMembers() {
			return members;
		}

		public void setMembers(Set<GroupMember> members) {
			this.members = members;
		}

		public void addMember(GroupMember groupMember) {
			this.members.add( groupMember );
		}
	}

	@Entity(name = "MultiGroup")
	@Audited
	public static class MultiGroup {
		@Id
		@GeneratedValue
		private Integer id;

		@ManyToMany
		@OrderColumn
		private List<GroupMember> members = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<GroupMember> getMembers() {
			return members;
		}

		public void setMembers(List<GroupMember> members) {
			this.members = members;
		}

		public void addMember(GroupMember groupMember) {
			this.members.add( groupMember );
		}
	}
}
