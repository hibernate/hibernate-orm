/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking;

import java.util.Objects;
import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({ DirtyCheckEnhancementContext.class })
@TestForIssue( jiraKey = "HHH-13766")
public class AttributeConverterTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( TestEntity.class );
	}

	private Long testEntityId;

	@Before
	public void setUp() {
		TestEntity entity = new TestEntity();
		inTransaction(
				session -> {
					TestData testData = new TestData();
					testData.setValue( "initial" );
					entity.setData( testData );
					session.save( entity );
				}
		);
		testEntityId = entity.getId();
	}

	@Test
	public void testUpdate() {
		inTransaction(
				session -> {
					TestEntity entity = session.find( TestEntity.class, testEntityId );
					entity.getData().setValue( "new" );

				}
		);

		inTransaction(
				session -> {
					TestEntity entity = session.find( TestEntity.class, testEntityId );
					assertThat( entity.getData().getValue(), is( "new" ) );
				}
		);

	}

	@Test
	public void testUpdate2() {
		inTransaction(
				session -> {
					TestEntity entity = session.find( TestEntity.class, testEntityId );
					TestData testData = new TestData();
					testData.setValue( "new" );
					entity.setData( testData );
				}
		);

		inTransaction(
				session -> {
					TestEntity entity = session.find( TestEntity.class, testEntityId );
					assertThat( entity.getData().getValue(), is( "new" ) );
				}
		);

	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(name = "id")
		private Long id;

		@NotNull
		@Convert(converter = TestConverter.class)
		@Column(name = "data")
		private TestData data;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public TestData getData() {
			return data;
		}

		public void setData(TestData data) {
			this.data = data;
		}
	}

	@Converter
	public static class TestConverter implements AttributeConverter<TestData, String> {

		@Override
		public String convertToDatabaseColumn(TestData attribute) {
			return attribute.getValue();
		}

		@Override
		public TestData convertToEntityAttribute(String dbData) {
			TestData testData = new TestData();
			testData.setValue( dbData );
			return testData;
		}
	}

	public static class TestData {
		private String value;

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			TestData testData = (TestData) o;
			return Objects.equals( value, testData.value );
		}

		@Override
		public int hashCode() {
			return Objects.hash( value );
		}
	}
}
