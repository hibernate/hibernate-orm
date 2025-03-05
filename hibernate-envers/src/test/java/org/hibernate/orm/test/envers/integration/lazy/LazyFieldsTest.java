package org.hibernate.orm.test.envers.integration.lazy;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.envers.Audited;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;

@JiraKey("")
@BytecodeEnhanced
@DomainModel(
		annotatedClasses = {
				LazyFieldsTest.TestEntity.class,
		}
)
@SessionFactory
public class LazyFieldsTest {

	private static final Long ID = 1L;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity testEntity = new TestEntity( ID, "test data", "lazyString", "group A" );
					session.persist( testEntity );
				}
		);
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, ID );
					testEntity.setData( "modified test data" );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Long id;

		@Audited
		@Basic(optional = false)
		private String data;

		@Audited
		@Basic(fetch = FetchType.LAZY)
		private String lazyString;

		@Audited
		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("a")
		private String lazyStringGroupA;

		public TestEntity() {
		}

		public TestEntity(Long id, String data, String lazyString, String anotherLazyString) {
			this.id = id;
			this.data = data;
			this.lazyString = lazyString;
			this.lazyStringGroupA = anotherLazyString;
		}

		public Long getId() {
			return id;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}

		public String getLazyString() {
			return lazyString;
		}

		public String getLazyStringGroupA() {
			return lazyStringGroupA;
		}
	}

}
