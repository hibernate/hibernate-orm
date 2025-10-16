/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.type.StandardBasicTypes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-7681")
@EnversTest
@Jpa(annotatedClasses = {
		GroupMemberTest.GroupMember.class,
		GroupMemberTest.MultiGroup.class,
		GroupMemberTest.UniqueGroup.class
})
public class GroupMemberTest {
	private Integer uniqueGroupId;
	private Integer groupMemberId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			final UniqueGroup uniqueGroup = new UniqueGroup();
			final GroupMember groupMember = new GroupMember();
			uniqueGroup.addMember( groupMember );
			em.persist( uniqueGroup );
			em.persist( groupMember );
			uniqueGroupId = uniqueGroup.getId();
			groupMemberId = groupMember.getId();
		} );
		// Revision 2
		scope.inTransaction( em -> {
			final GroupMember groupMember = em.find( GroupMember.class, groupMemberId );
			final MultiGroup multiGroup = new MultiGroup();
			groupMember.addMultiGroup( multiGroup );
			em.persist( multiGroup );
		} );
	}

	@Test
	public void testUniqueGroupFound(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			final GroupMember groupMember = em.find( GroupMember.class, groupMemberId );
			assertNotNull( groupMember );
			assertNotNull( groupMember.getUniqueGroup() );
		} );
	}

	@Test
	public void testUniqueGroupFromAuditHistory(EntityManagerFactoryScope scope) {
		assertEquals( uniqueGroupId, getCurrentAuditUniqueGroupId( scope ) );
	}

	private Integer getCurrentAuditUniqueGroupId(EntityManagerFactoryScope scope) {
		return scope.fromTransaction( em -> {
			final var session = em.unwrap( org.hibernate.Session.class );
			final var query = session.createNativeQuery(
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
