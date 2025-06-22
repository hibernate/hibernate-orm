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
		GenericEmbeddedPropertyMappedSuperclassTest.EmbeddableProp.class,
		GenericEmbeddedPropertyMappedSuperclassTest.AccessReport.class,
		GenericEmbeddedPropertyMappedSuperclassTest.Group.class,
		GenericEmbeddedPropertyMappedSuperclassTest.User.class,
		GenericEmbeddedPropertyMappedSuperclassTest.UserReport.class,
		GenericEmbeddedPropertyMappedSuperclassTest.UserAccessReport.class,
		GenericEmbeddedPropertyMappedSuperclassTest.UserAccessReportEntity.class,
		GenericEmbeddedPropertyMappedSuperclassTest.GroupAccessReport.class,
		GenericEmbeddedPropertyMappedSuperclassTest.GroupAccessReportEntity.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16491" )
public class GenericEmbeddedPropertyMappedSuperclassTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final User user = new User( "user" );
			session.persist( user );
			final UserAccessReportEntity userAccessReportEntity = new UserAccessReportEntity();
			userAccessReportEntity.setProp( new EmbeddableProp<>( user, new UserReport( "user_report" ), 1 ) );
			session.persist( userAccessReportEntity );
			final Group group = new Group( "group" );
			session.persist( group );
			final GroupAccessReportEntity groupAccessReportEntity = new GroupAccessReportEntity();
			groupAccessReportEntity.setProp( new EmbeddableProp<>( group, new GroupReport( "group_report" ), 2 ) );
			session.persist( groupAccessReportEntity );
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
			final UserAccessReportEntity result = session.createQuery(
					"select ur from UserAccessReport ur " +
					"where ur.prop.entity.login = 'user' " +
					"and ur.prop.embedded.userCode = 'user_report'",
					UserAccessReportEntity.class
			).getSingleResult();
			assertThat( result.getProp().getEntity().getLogin() ).isEqualTo( "user" );
			assertThat( result.getProp().getEmbedded().getUserCode() ).isEqualTo( "user_report" );
		} );
	}

	@Test
	public void testUserReportCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<UserAccessReportEntity> query = cb.createQuery( UserAccessReportEntity.class );
			final Root<UserAccessReportEntity> root = query.from( UserAccessReportEntity.class );
			final Path<Object> prop = root.get( "prop" );
			assertThat( prop.getJavaType() ).isEqualTo( EmbeddableProp.class );
			// assert that the generic attributes inside the component's model are reported as Object type
			final SqmPathSource<?> modelPathSource = (SqmPathSource<?>) prop.getModel();
			assertThat( modelPathSource.findSubPathSource( "entity" ).getBindableJavaType() ).isEqualTo( Object.class );
			assertThat( modelPathSource.findSubPathSource( "embedded" )
								.getBindableJavaType() ).isEqualTo( Object.class );
			// the serial property is not generic, so it should have the correct type even in the generic component's model
			assertThat( modelPathSource.findSubPathSource( "serial" )
								.getBindableJavaType() ).isEqualTo( Integer.class );
			// assert that types of the concrete attributes inside the component's resolved model are reported correctly
			final SqmPathSource<?> resolvedPathSource = ( (SqmPath<Object>) prop ).getResolvedModel();
			assertThat( resolvedPathSource.findSubPathSource( "entity" )
								.getBindableJavaType() ).isEqualTo( User.class );
			assertThat( resolvedPathSource.findSubPathSource( "embedded" )
								.getBindableJavaType() ).isEqualTo( UserReport.class );
			assertThat( modelPathSource.findSubPathSource( "serial" )
								.getBindableJavaType() ).isEqualTo( Integer.class );
			// test same query as HQL
			query.select( root ).where(
					cb.and(
							cb.equal( prop.get( "entity" ).get( "login" ), "user" ),
							cb.equal( prop.get( "embedded" ).get( "userCode" ), "user_report" )
					)
			);
			final UserAccessReportEntity result = session.createQuery( query ).getSingleResult();
			assertThat( result.getProp().getEntity().getLogin() ).isEqualTo( "user" );
			assertThat( result.getProp().getEmbedded().getUserCode() ).isEqualTo( "user_report" );
		} );
	}

	@Test
	public void testGroupReport(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final GroupAccessReportEntity result = session.createQuery(
					"select gr from GroupAccessReport gr " +
					"where gr.prop.entity.name = 'group' " +
					"and gr.prop.embedded.groupCode = 'group_report'",
					GroupAccessReportEntity.class
			).getSingleResult();
			assertThat( result.getProp().getEntity().getName() ).isEqualTo( "group" );
			assertThat( result.getProp().getEmbedded().getGroupCode() ).isEqualTo( "group_report" );
		} );
	}

	@Test
	public void testGroupReportCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<GroupAccessReportEntity> query = cb.createQuery( GroupAccessReportEntity.class );
			final Root<GroupAccessReportEntity> root = query.from( GroupAccessReportEntity.class );
			final Path<Object> prop = root.get( "prop" );
			assertThat( prop.getJavaType() ).isEqualTo( EmbeddableProp.class );
			// assert that the generic attributes inside the component's model are reported as Object type
			final SqmPathSource<?> modelPathSource = (SqmPathSource<?>) prop.getModel();
			assertThat( modelPathSource.findSubPathSource( "entity" ).getBindableJavaType() ).isEqualTo( Object.class );
			assertThat( modelPathSource.findSubPathSource( "embedded" )
								.getBindableJavaType() ).isEqualTo( Object.class );
			// the serial property is not generic, so it should have the correct type even in the generic component's model
			assertThat( modelPathSource.findSubPathSource( "serial" )
								.getBindableJavaType() ).isEqualTo( Integer.class );
			// assert that types of the concrete attributes inside the component's resolved model are reported correctly
			final SqmPathSource<?> resolvedPathSource = ( (SqmPath<Object>) prop ).getResolvedModel();
			assertThat( resolvedPathSource.findSubPathSource( "entity" )
								.getBindableJavaType() ).isEqualTo( Group.class );
			assertThat( resolvedPathSource.findSubPathSource( "embedded" )
								.getBindableJavaType() ).isEqualTo( GroupReport.class );
			assertThat( modelPathSource.findSubPathSource( "serial" )
								.getBindableJavaType() ).isEqualTo( Integer.class );
			// test same query as HQL
			query.select( root ).where(
					cb.and(
							cb.equal( prop.get( "entity" ).get( "name" ), "group" ),
							cb.equal( prop.get( "embedded" ).get( "groupCode" ), "group_report" )
					)
			);
			final GroupAccessReportEntity result = session.createQuery( query ).getSingleResult();
			assertThat( result.getProp().getEntity().getName() ).isEqualTo( "group" );
			assertThat( result.getProp().getEmbedded().getGroupCode() ).isEqualTo( "group_report" );
		} );
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
	public static class User {
		private Long id;
		private String login;

		public User() {
		}

		public User(String login) {
			this.login = login;
		}

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
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
	public static class Group {
		private Long id;
		private String name;

		public Group() {
		}

		public Group(String name) {
			this.name = name;
		}

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static abstract class GenericObject<T extends Serializable> {
		private T prop;

		public T getProp() {
			return prop;
		}

		public void setProp(T prop) {
			this.prop = prop;
		}
	}

	@Embeddable
	public static class EmbeddableProp<O, E> implements Serializable {
		private O entity;
		private E embedded;
		private Integer serial;

		public EmbeddableProp() {
		}

		public EmbeddableProp(O entity, E embedded, Integer serial) {
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
	public static abstract class AccessReport<O, E> extends GenericObject<EmbeddableProp<O, E>> {
		private Long id;

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@MappedSuperclass
	public static class UserAccessReport extends AccessReport<User, UserReport> {
		@Override
		@Embedded
		@AssociationOverrides( @AssociationOverride( name = "entity", joinColumns = @JoinColumn( name = "user_id" ) ) )
		public EmbeddableProp<User, UserReport> getProp() {
			return super.getProp();
		}

		@Override
		public void setProp(EmbeddableProp<User, UserReport> key) {
			super.setProp( key );
		}
	}

	@Entity( name = "UserAccessReport" )
	public static class UserAccessReportEntity extends UserAccessReport {
	}

	@MappedSuperclass
	public static class GroupAccessReport extends AccessReport<Group, GroupReport> {
		@Override
		@Embedded
		@AssociationOverrides( @AssociationOverride( name = "entity", joinColumns = @JoinColumn( name = "group_id" ) ) )
		public EmbeddableProp<Group, GroupReport> getProp() {
			return super.getProp();
		}

		@Override
		public void setProp(EmbeddableProp<Group, GroupReport> key) {
			super.setProp( key );
		}
	}

	@Entity( name = "GroupAccessReport" )
	public static class GroupAccessReportEntity extends GroupAccessReport {
	}
}
