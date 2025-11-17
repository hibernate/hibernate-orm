/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytoone.lazy;

import jakarta.persistence.Column;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author Jan Schatteman
 */
@EnversTest
@Jpa (
		annotatedClasses = {
				ManyToOneLazyProxyTest.OtherEntity.class, ManyToOneLazyProxyTest.MyEntity.class
		},
		integrationSettings = {
				@Setting(
						name = AvailableSettings.STATEMENT_INSPECTOR,
						value = "org.hibernate.testing.jdbc.SQLStatementInspector"
				)
		}
)
@JiraKey("HHH-14176")
public class ManyToOneLazyProxyTest {

	@AfterEach
	public void clearData(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
						entityManager.createQuery( "delete from MyEntity" );
						entityManager.createQuery( "delete from OtherEntity" );
					}
		);
	}

	@Test
	public void testLazyProxy(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					OtherEntity other = new OtherEntity();
					other.setId( 1L );
					other.setDesc( "abc" );
					entityManager.persist( other );
				}
		);

		SQLStatementInspector sqlStatementInspector = scope.getStatementInspector( SQLStatementInspector.class );
		sqlStatementInspector.clear();
		scope.inTransaction(
				entityManager -> {
					MyEntity me = new MyEntity();
					me.setOther( entityManager.getReference( OtherEntity.class, 1L ) );
					entityManager.persist(me);
				}
		);
		Assertions.assertEquals( 0, sqlStatementInspector.getSqlQueries().stream().filter( s -> s.startsWith( "select" ) && s.contains( "other_entity" ) ).count() );

	}

	@Entity(name = "OtherEntity")
	@Table(name = "other_entity")
	@Audited
	@AuditTable(value = "other_entity_aud")
	public static class OtherEntity {
		@Id
		private Long id;

		@Column(name = "description")
		private String desc;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getDesc() {
			return desc;
		}

		public void setDesc(String desc) {
			this.desc = desc;
		}
	}

	@Entity(name = "MyEntity")
	@Table(name = "my_entity")
	@Audited
	@AuditTable(value = "my_entity_aud")
	public static class MyEntity {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		private OtherEntity other;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public OtherEntity getOther() {
			return other;
		}

		public void setOther(OtherEntity other) {
			this.other = other;
		}
	}

}
