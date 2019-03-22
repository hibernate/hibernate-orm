/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.multiplerelations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import org.hibernate.Session;
import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.type.Type;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-7681")
@Disabled("NYI - Native Query Support - LegacyResultSetMappingDescriptor")
public class GroupMemberTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer uniqueGroupId;
	private Integer groupMemberId;

    @Override
    protected Class<?>[] getAnnotatedClasses() {
    	return new Class[] { GroupMember.class, MultiGroup.class, UniqueGroup.class };
    }

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final UniqueGroup uniqueGroup = new UniqueGroup();
					final GroupMember groupMember = new GroupMember();
					uniqueGroup.addMember( groupMember );
					entityManager.persist( uniqueGroup );
					entityManager.persist( groupMember );
					uniqueGroupId = uniqueGroup.getId();
					groupMemberId = groupMember.getId();
				},

				// Revision 2
				entityManager -> {
					final GroupMember groupMember = entityManager.find( GroupMember.class, groupMemberId );
					final MultiGroup multiGroup = new MultiGroup();
					groupMember.addMultiGroup( multiGroup );
					entityManager.persist( multiGroup );
				}
		);
	}

	@DynamicTest
	public void testUniqueGroupFound() {
    	inTransaction(
    			entityManager -> {
					final GroupMember groupMember = entityManager.find( GroupMember.class, groupMemberId );
					assertNotNull( groupMember );
					assertNotNull( groupMember.getUniqueGroup() );
				}
		);
	}

	@DynamicTest
	public void testUniqueGroupFromAuditHistory() {
    	assertThat( getCurrentAuditUniqueGroupId(), equalTo( uniqueGroupId ) );
	}

	private Integer getCurrentAuditUniqueGroupId() {
    	return inTransaction(
    			entityManager -> {
    				final Object result = entityManager
							.unwrap( Session.class )
							.createNativeQuery( "SELECT uniqueGroup_id From GroupMember_AUD ORDER BY rev DESC" )
							.addScalar( "uniqueGroup_id", (Type) StandardSpiBasicTypes.INTEGER )
							.setMaxResults( 1 )
							.getSingleResult();
    				assertThat( result, notNullValue() );
    				return (Integer) result;
				}
		);
	}

	@Entity(name = "GroupMember")
	@Audited
	public static class GroupMember {
		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne
		@JoinColumn(name = "uniqueGroup_id", insertable = false, updatable = false)
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
		@JoinColumn(name = "uniqueGroup_id")
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
