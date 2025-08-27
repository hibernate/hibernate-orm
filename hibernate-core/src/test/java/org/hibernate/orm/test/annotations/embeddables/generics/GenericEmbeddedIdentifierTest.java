/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embeddables.generics;

import java.io.Serializable;

import org.hibernate.query.sqm.SqmPathSource;

import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		GenericEmbeddedIdentifierTest.EmbeddableKey.class,
		GenericEmbeddedIdentifierTest.AccessReport.class,
		GenericEmbeddedIdentifierTest.Group.class,
		GenericEmbeddedIdentifierTest.User.class,
		GenericEmbeddedIdentifierTest.UserReport.class,
		GenericEmbeddedIdentifierTest.GroupReport.class,
		GenericEmbeddedIdentifierTest.GroupAccessReport.class,
		GenericEmbeddedIdentifierTest.UserAccessReport.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16491" )
public class  GenericEmbeddedIdentifierTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final User user = new User( "user" );
			session.persist( user );
			final UserAccessReport userAccessReport = new UserAccessReport();
			userAccessReport.setId( new EmbeddableKey<>( user, new UserReport( "user_report" ), 1 ) );
			session.persist( userAccessReport );
			final Group group = new Group( "group" );
			session.persist( group );
			final GroupAccessReport groupAccessReport = new GroupAccessReport();
			groupAccessReport.setId( new EmbeddableKey<>( group, new GroupReport( "group_report" ), 2 ) );
			session.persist( groupAccessReport );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from GroupAccessReport" ).executeUpdate();
			session.createMutationQuery( "delete from UserAccessReport" ).executeUpdate();
			session.createMutationQuery( "delete from GroupEntity" ).executeUpdate();
			session.createMutationQuery( "delete from UserEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testUserReport(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final UserAccessReport result = session.createQuery(
					"select ur from UserAccessReport ur " +
					"where ur.id.entity.login = 'user' " +
					"and ur.id.embedded.userCode = 'user_report'",
					UserAccessReport.class
			).getSingleResult();
			assertThat( result.getId().getEntity().getLogin() ).isEqualTo( "user" );
			assertThat( result.getId().getEmbedded().getUserCode() ).isEqualTo( "user_report" );
		} );
	}

	@Test
	public void testUserReportCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<UserAccessReport> query = cb.createQuery( UserAccessReport.class );
			final Root<UserAccessReport> root = query.from( UserAccessReport.class );
			final Path<Object> id = root.get( "id" );
			assertThat( id.getJavaType() ).isEqualTo( EmbeddableKey.class );
			// assert that the generic attributes inside the component's model are reported as Object type
			final SqmPathSource<?> modelPathSource = (SqmPathSource<?>) id.getModel();
			assertThat( modelPathSource.findSubPathSource( "entity" ).getBindableJavaType() ).isEqualTo( Object.class );
			assertThat( modelPathSource.findSubPathSource( "embedded" )
								.getBindableJavaType() ).isEqualTo( Object.class );
			// the serial property is not generic, so it should have the correct type even in the generic component's model
			assertThat( modelPathSource.findSubPathSource( "serial" )
								.getBindableJavaType() ).isEqualTo( Integer.class );
			// assert that types of the concrete attributes inside the component's resolved model are reported correctly
			final SqmPathSource<?> resolvedPathSource = ( (SqmPath<Object>) id ).getResolvedModel();
			assertThat( resolvedPathSource.findSubPathSource( "entity" )
								.getBindableJavaType() ).isEqualTo( User.class );
			assertThat( resolvedPathSource.findSubPathSource( "embedded" )
								.getBindableJavaType() ).isEqualTo( UserReport.class );
			assertThat( resolvedPathSource.findSubPathSource( "serial" )
								.getBindableJavaType() ).isEqualTo( Integer.class );
			// test same query as HQL
			query.select( root ).where(
					cb.and(
							cb.equal( id.get( "entity" ).get( "login" ), "user" ),
							cb.equal( id.get( "embedded" ).get( "userCode" ), "user_report" )
					)
			);
			final UserAccessReport result = session.createQuery( query ).getSingleResult();
			assertThat( result.getId().getEntity().getLogin() ).isEqualTo( "user" );
			assertThat( result.getId().getEmbedded().getUserCode() ).isEqualTo( "user_report" );
		} );
	}

	@Test
	public void testGroupReport(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final GroupAccessReport result = session.createQuery(
					"select gr from GroupAccessReport gr " +
					"where gr.id.entity.name = 'group'" +
					"and gr.id.embedded.groupCode = 'group_report'",
					GroupAccessReport.class
			).getSingleResult();
			assertThat( result.getId().getEntity().getName() ).isEqualTo( "group" );
			assertThat( result.getId().getEmbedded().getGroupCode() ).isEqualTo( "group_report" );
		} );
	}

	@Test
	public void testGroupReportCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<GroupAccessReport> query = cb.createQuery( GroupAccessReport.class );
			final Root<GroupAccessReport> root = query.from( GroupAccessReport.class );
			final Path<Object> id = root.get( "id" );
			assertThat( id.getJavaType() ).isEqualTo( EmbeddableKey.class );
			// assert that the generic attributes inside the component's model are reported as Object type
			final SqmPathSource<?> modelPathSource = (SqmPathSource<?>) id.getModel();
			assertThat( modelPathSource.findSubPathSource( "entity" ).getBindableJavaType() ).isEqualTo( Object.class );
			assertThat( modelPathSource.findSubPathSource( "embedded" )
								.getBindableJavaType() ).isEqualTo( Object.class );
			// the serial property is not generic, so it should have the correct type even in the generic component's model
			assertThat( modelPathSource.findSubPathSource( "serial" )
								.getBindableJavaType() ).isEqualTo( Integer.class );
			// assert that types of the concrete attributes inside the component's resolved model are reported correctly
			final SqmPathSource<?> resolvedPathSource = ( (SqmPath<Object>) id ).getResolvedModel();
			assertThat( resolvedPathSource.findSubPathSource( "entity" )
								.getBindableJavaType() ).isEqualTo( Group.class );
			assertThat( resolvedPathSource.findSubPathSource( "embedded" )
								.getBindableJavaType() ).isEqualTo( GroupReport.class );
			assertThat( resolvedPathSource.findSubPathSource( "serial" )
								.getBindableJavaType() ).isEqualTo( Integer.class );
			// test same query as HQL
			query.select( root ).where(
					cb.and(
							cb.equal( id.get( "entity" ).get( "name" ), "group" ),
							cb.equal( id.get( "embedded" ).get( "groupCode" ), "group_report" )
					)
			);
			final GroupAccessReport result = session.createQuery( query ).getSingleResult();
			assertThat( result.getId().getEntity().getName() ).isEqualTo( "group" );
			assertThat( result.getId().getEmbedded().getGroupCode() ).isEqualTo( "group_report" );
		} );
	}

	public static abstract class GenericObject<ID extends Serializable> {
		private ID id;

		public ID getId() {
			return id;
		}

		public void setId(ID id) {
			this.id = id;
		}
	}

	@Embeddable
	public static class UserReport {
		private String userCode;

		public UserReport() {
		}

		public UserReport(String userCode) {
			this.userCode = userCode;
		}

		public String getUserCode() {
			return userCode;
		}

		public void setUserCode(String code) {
			this.userCode = code;
		}
	}

	@Entity( name = "UserEntity" )
	public static class User extends GenericObject<Long> {
		private String login;

		public User() {
		}

		public User(String login) {
			this.login = login;
		}

		@Override
		@Id
		@GeneratedValue
		public Long getId() {
			return super.getId();
		}

		public void setId(Long id) {
			super.setId( id );
		}

		public String getLogin() {
			return login;
		}

		public void setLogin(String login) {
			this.login = login;
		}
	}

	@Embeddable
	public static class GroupReport {
		private String groupCode;

		public GroupReport() {
		}

		public GroupReport(String groupCode) {
			this.groupCode = groupCode;
		}

		public String getGroupCode() {
			return groupCode;
		}

		public void setGroupCode(String code) {
			this.groupCode = code;
		}
	}

	@Entity( name = "GroupEntity" )
	public static class Group extends GenericObject<Long> {
		private String name;

		public Group() {
		}

		public Group(String name) {
			this.name = name;
		}

		@Override
		@Id
		@GeneratedValue
		public Long getId() {
			return super.getId();
		}

		public void setId(Long id) {
			super.setId( id );
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public static class EmbeddableKey<O, E> implements Serializable {
		private O entity;
		private E embedded;
		private Integer serial;

		public EmbeddableKey() {
		}

		public EmbeddableKey(O entity, E embedded, Integer serial) {
			this.entity = entity;
			this.embedded = embedded;
			this.serial = serial;
		}

		@ManyToOne
		public O getEntity() {
			return entity;
		}

		public void setEntity(O entity) {
			this.entity = entity;
		}

		@Embedded
		public E getEmbedded() {
			return embedded;
		}

		public void setEmbedded(E embedded) {
			this.embedded = embedded;
		}

		public Integer getSerial() {
			return serial;
		}

		public void setSerial(Integer serial) {
			this.serial = serial;
		}
	}

	@MappedSuperclass
	public static abstract class AccessReport<O, E> extends GenericObject<EmbeddableKey<O, E>> {
	}

	@Entity( name = "UserAccessReport" )
	public static class UserAccessReport extends AccessReport<User, UserReport> {
		@Override
		@EmbeddedId
		@AssociationOverrides( @AssociationOverride( name = "entity", joinColumns = @JoinColumn( name = "user_id" ) ) )
		public EmbeddableKey<User, UserReport> getId() {
			return super.getId();
		}

		@Override
		public void setId(EmbeddableKey<User, UserReport> key) {
			super.setId( key );
		}
	}

	@Entity( name = "GroupAccessReport" )
	public static class GroupAccessReport extends AccessReport<Group, GroupReport> {
		@Override
		@EmbeddedId
		@AssociationOverrides( @AssociationOverride( name = "entity", joinColumns = @JoinColumn( name = "group_id" ) ) )
		public EmbeddableKey<Group, GroupReport> getId() {
			return super.getId();
		}

		@Override
		public void setId(EmbeddableKey<Group, GroupReport> key) {
			super.setId( key );
		}
	}
}
