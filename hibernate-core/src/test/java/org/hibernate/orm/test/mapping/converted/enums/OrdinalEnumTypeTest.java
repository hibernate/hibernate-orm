/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.enums;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.JdbcBindingLogging;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.MessageKeyInspection;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihacea
 */
@MessageKeyInspection(
		logger = @Logger( loggerName = JdbcBindingLogging.NAME ),
		messageKey = "binding parameter"
)
@DomainModel( annotatedClasses = OrdinalEnumTypeTest.Person.class )
@SessionFactory
public class OrdinalEnumTypeTest {
	@BeforeEach
	protected void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final Person person = Person.person( Gender.MALE, HairColor.BROWN );
					session.persist( person );
					session.persist( Person.person( Gender.MALE, HairColor.BLACK ) );
					session.persist( Person.person( Gender.FEMALE, HairColor.BROWN ) );
					session.persist( Person.person( Gender.FEMALE, HairColor.BLACK ) );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-12978")
	public void testEnumAsBindParameterAndExtract(SessionFactoryScope scope, MessageKeyWatcher loggingWatcher) {
		scope.inTransaction(
				(session) -> {
					session.createQuery( "select p.id from Person p where p.id = :id", Long.class )
							.setParameter( "id", 1L )
							.list();

					assertTrue( loggingWatcher.wasTriggered() );
				}
		);

		loggingWatcher.reset();

		scope.inTransaction(
				(session) -> {
					final String qry = "select p.gender from Person p where p.gender = :gender and p.hairColor = :hairColor";
					session.createQuery( qry, Gender.class )
							.setParameter( "gender", Gender.MALE )
							.setParameter( "hairColor", HairColor.BROWN )
							.getSingleResult();

					assertTrue( loggingWatcher.wasTriggered() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10282")
	public void hqlTestEnumShortHandSyntax(SessionFactoryScope scope, MessageKeyWatcher loggingWatcher) {
		scope.inTransaction(
				(session) -> {
					session.createQuery(
							"select id from Person where originalHairColor = BLONDE")
							.getResultList();

					assertTrue( loggingWatcher.wasTriggered() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10282")
	public void hqlTestEnumQualifiedShortHandSyntax(SessionFactoryScope scope, MessageKeyWatcher loggingWatcher) {
		final String qry = "select id from Person where originalHairColor = HairColor.BLONDE";
		scope.inTransaction(
				(session) -> {
					session.createQuery( qry ).getResultList();

					assertTrue( loggingWatcher.wasTriggered() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10282")
	public void hqlTestEnumShortHandSyntaxInPredicate(SessionFactoryScope scope, MessageKeyWatcher loggingWatcher) {
		scope.inTransaction(
				(session) -> {
					final String qry = "select id from Person where originalHairColor in (BLONDE, BROWN)";
					session.createQuery( qry ).getResultList();

					assertTrue( loggingWatcher.wasTriggered() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10282")
	public void hqlTestEnumQualifiedShortHandSyntaxInPredicate(SessionFactoryScope scope, MessageKeyWatcher loggingWatcher) {
		scope.inTransaction(
				(session) -> {
					final String qry = "select id from Person where originalHairColor in (HairColor.BLONDE, HairColor.BROWN)";
					session.createQuery( qry ).getResultList();

					assertTrue( loggingWatcher.wasTriggered() );
				}
		);
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-19276" )
	public void testNoEnumMemoryLeak(SessionFactoryScope scope) {
		final List<HairColor> colors = List.of(HairColor.BLACK, HairColor.BROWN);

		final TypeConfiguration typeConfiguration = scope.getSessionFactory().getTypeConfiguration();
		final TestingBasicTypeRegistry basicTypeRegistry = new TestingBasicTypeRegistry( typeConfiguration.getBasicTypeRegistry() );
		final Map<JdbcType, Map<JavaType<?>, BasicType<?>>> registryValues = basicTypeRegistry.getRegistryValues();
		final Map<JavaType<?>, BasicType<?>> enumJavaTypeValues = registryValues.get( typeConfiguration.getJdbcTypeRegistry().findDescriptor( SqlTypes.TINYINT ) );

		checkEnumTypeRegistryValues( enumJavaTypeValues );

		// Basically, multiple runs of this should not result in the creation of additional
		// EnumJavaTypes (or BasicTypes for that matter), as was the case before the fix for HHH-19276
		for (int counter = 1; counter <= 10; counter++ ) {
			scope.inTransaction(
					session -> session.createNativeQuery(
							"SELECT * FROM Person WHERE hairColor in (:colors)",
							Person.class
					)
					.setParameter( "colors", colors )
					.list()
			);
		}

		checkEnumTypeRegistryValues( enumJavaTypeValues );
	}

	private void checkEnumTypeRegistryValues(Map<JavaType<?>, BasicType<?>> values) {
		assertEquals( 2, values.size() );
		boolean genderAccountedFor = false;
		boolean hairColorAccountedFor = false;
		for (JavaType<?> type : values.keySet()) {
			genderAccountedFor = genderAccountedFor || type.getTypeName().equals( "org.hibernate.orm.test.mapping.converted.enums.Gender" );
			hairColorAccountedFor = hairColorAccountedFor || type.getTypeName().equals( "org.hibernate.orm.test.mapping.converted.enums.HairColor" );
		}
		assertTrue( genderAccountedFor && hairColorAccountedFor );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@Enumerated(EnumType.ORDINAL)
		private Gender gender;

		@Enumerated(EnumType.ORDINAL)
		private HairColor hairColor;

		@Enumerated(EnumType.ORDINAL)
		private HairColor originalHairColor;

		public static Person person(Gender gender, HairColor hairColor) {
			Person person = new Person();
			person.setGender( gender );
			person.setHairColor( hairColor );
			return person;
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public Gender getGender() {
			return gender;
		}

		public void setGender(Gender gender) {
			this.gender = gender;
		}

		public HairColor getHairColor() {
			return hairColor;
		}

		public void setHairColor(HairColor hairColor) {
			this.hairColor = hairColor;
		}

		public HairColor getOriginalHairColor() {
			return originalHairColor;
		}

		public void setOriginalHairColor(HairColor originalHairColor) {
			this.originalHairColor = originalHairColor;
		}
	}

	private static final class TestingBasicTypeRegistry extends BasicTypeRegistry {
		private TestingBasicTypeRegistry(BasicTypeRegistry basicTypeRegistry){
			super(basicTypeRegistry);
		}

		@Override
		protected Map<JdbcType, Map<JavaType<?>, BasicType<?>>> getRegistryValues() {
			return super.getRegistryValues();
		}
	}
}
