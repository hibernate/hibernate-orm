/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batchfetch;

import java.time.ZonedDateTime;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ImplicitListAsBagProvider;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@DomainModel(
		annotatedClasses = {
				BatchFetchReferencedColumnNameTest.Child.class,
				BatchFetchReferencedColumnNameTest.Parent.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
				@Setting(name = AvailableSettings.FORMAT_SQL, value = "true"),
				@Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "64")
		},
		settingProviders = @SettingProvider(
				settingName = DEFAULT_LIST_SEMANTICS,
				provider = ImplicitListAsBagProvider.class
		)
)
public class BatchFetchReferencedColumnNameTest {

	@Test
	@JiraKey(value = "HHH-13059")
	public void test(SessionFactoryScope scope) throws Exception {
		scope.inTransaction( session -> {
			Parent p = new Parent();
			p.setId( 1L );
			session.persist( p );

			Child c1 = new Child();
			c1.setCreatedOn( ZonedDateTime.now() );
			c1.setParentId( 1L );
			c1.setId( 10L );
			session.persist( c1 );

			Child c2 = new Child();
			c2.setCreatedOn( ZonedDateTime.now() );
			c2.setParentId( 1L );
			c2.setId( 11L );
			session.persist( c2 );
		} );

		scope.inTransaction( session -> {
			Parent p = session.get( Parent.class, 1L );
			assertNotNull( p );

			assertEquals( 2, p.getChildren().size() );
		} );
	}

	@Entity
	@Table(name = "CHILD")
	public static class Child {

		@Id
		@Column(name = "CHILD_ID")
		private Long id;

		@Column(name = "PARENT_ID")
		private Long parentId;

		@Column(name = "CREATED_ON")
		private ZonedDateTime createdOn;

		public ZonedDateTime getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(ZonedDateTime createdOn) {
			this.createdOn = createdOn;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getParentId() {
			return parentId;
		}

		public void setParentId(Long parentId) {
			this.parentId = parentId;
		}
	}

	@Entity
	@Table(name = "PARENT")
	public static class Parent {

		@Id
		@Column(name = "PARENT_ID")
		private Long id;

		@OneToMany(fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
		@JoinColumn(name = "PARENT_ID", referencedColumnName = "PARENT_ID")
		@OrderBy("createdOn desc")
		private List<Child> children;

		public List<Child> getChildren() {
			return children;
		}

		public void setChildren(List<Child> children) {
			this.children = children;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

	}
}
