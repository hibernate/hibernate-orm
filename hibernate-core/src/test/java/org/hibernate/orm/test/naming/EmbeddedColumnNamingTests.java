/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.naming;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.EmbeddedColumnNaming;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@Jira( "https://hibernate.atlassian.net/browse/HHH-4396" )
public class EmbeddedColumnNamingTests {
	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = {Address.class,BadPerson.class})
	void testNoNamingPattern(DomainModelScope domainModelScope) {
		final PersistentClass entityBinding = domainModelScope.getEntityBinding( BadPerson.class );
		try {
			entityBinding.validate( domainModelScope.getDomainModel() );
			fail( "Expecting an exception" );
		}
		catch (MappingException expected) {
			// expected outcome
			assertThat( expected.getMessage() ).contains( "when mapping multiple properties to the same column" );
		}
	}

	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = {Address.class,GoodPerson.class})
	@FailureExpected( jiraKey = "HHH-4396", reason = "@EmbeddedColumnNaming support is not implemented yet" )
	void testGoodNamingPattern(DomainModelScope domainModelScope) {
		final PersistentClass entityBinding = domainModelScope.getEntityBinding( GoodPerson.class );
		entityBinding.validate( domainModelScope.getDomainModel() );

		final Property homeAddress = entityBinding.getProperty( "homeAddress" );
		final Component homeAddressValue = (Component) homeAddress.getValue();

		final Property workAddress = entityBinding.getProperty( "workAddress" );
		final Component workAddressValue = (Component) workAddress.getValue();
	}

	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = {Address.class,BadPatternPerson1.class})
	@FailureExpected( jiraKey = "HHH-4396", reason = "@EmbeddedColumnNaming support is not implemented yet" )
	void testBadNamingPattern1(DomainModelScope domainModelScope) {
		final PersistentClass entityBinding = domainModelScope.getEntityBinding( BadPatternPerson1.class );
		try {
			entityBinding.validate( domainModelScope.getDomainModel() );
			fail( "Expecting an exception" );
		}
		catch (MappingException expected) {
			// expected outcome
			assertThat( expected.getMessage() ).contains( "bad embedded naming pattern" );
		}
	}

	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = {Address.class,BadPatternPerson2.class})
	@FailureExpected( jiraKey = "HHH-4396", reason = "@EmbeddedColumnNaming support is not implemented yet" )
	void testBadNamingPattern2(DomainModelScope domainModelScope) {
		final PersistentClass entityBinding = domainModelScope.getEntityBinding( BadPatternPerson2.class );
		try {
			entityBinding.validate( domainModelScope.getDomainModel() );
			fail( "Expecting an exception" );
		}
		catch (MappingException expected) {
			// expected outcome
			assertThat( expected.getMessage() ).contains( "bad embedded naming pattern" );
		}
	}

	@Embeddable
	public static class Address {
		private String street;
		private String city;
		private String state;
		private String zip;
	}

	@Entity(name="BadPerson")
	@Table(name="bad_person")
	public static class BadPerson {
		@Id
		private Integer id;
		private String name;

		@Embedded
		private Address homeAddress;

		@Embedded
		private Address workAddress;
	}

	@Entity(name="GoodPerson")
	@Table(name="good_person")
	public static class GoodPerson {
		@Id
		private Integer id;
		private String name;

		@Embedded
		@EmbeddedColumnNaming("home_%s")
		private Address homeAddress;

		@Embedded
		@EmbeddedColumnNaming("work_%s")
		private Address workAddress;
	}

	@Entity(name="BadPatternPerson1")
	@Table(name="bad_pattern_person_1")
	public static class BadPatternPerson1 {
		@Id
		private Integer id;
		private String name;

		@Embedded
		@EmbeddedColumnNaming("%s_home_%s")
		private Address homeAddress;

		@Embedded
		@EmbeddedColumnNaming("work_%s")
		private Address workAddress;
	}

	@Entity(name="BadPatternPerson2")
	@Table(name="bad_pattern_person_2")
	public static class BadPatternPerson2 {
		@Id
		private Integer id;
		private String name;

		@Embedded
		@EmbeddedColumnNaming("home")
		private Address homeAddress;

		@Embedded
		@EmbeddedColumnNaming("work_%s")
		private Address workAddress;
	}
}
