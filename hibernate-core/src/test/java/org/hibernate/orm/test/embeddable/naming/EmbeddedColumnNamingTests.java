/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable.naming;

import org.hibernate.MappingException;
import org.hibernate.annotations.EmbeddedColumnNaming;
import org.hibernate.boot.MetadataSources;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
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
	/**
	 * Without {@code @EmbeddedColumnNaming} we end up with a name clash
	 */
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

	/**
	 * Simple use of {@code @EmbeddedColumnNaming} on a single embedded attribute
	 */
	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = {Address.class,BaselinePerson.class})
	@SessionFactory
	void testBaselineNamingPattern(SessionFactoryScope sfScope) {
		final SessionFactoryImplementor sessionFactory = sfScope.getSessionFactory();
		final MappingMetamodelImplementor mappingMetamodel = sessionFactory.getMappingMetamodel();
		final EntityPersister persister = mappingMetamodel.getEntityDescriptor( BaselinePerson.class );
		verifyColumnNames( persister.findAttributeMapping( "homeAddress" ), "home_" );
	}

	public static void verifyColumnNames(AttributeMapping embeddedMapping, String prefix) {
		embeddedMapping.forEachSelectable( (selectionIndex, selectableMapping) -> {
			assertThat( selectableMapping.getSelectionExpression() ).startsWith( prefix );
		} );
	}

	/**
	 * Simple use of {@code @EmbeddedColumnNaming} on one embedded attribute leaving a second as default
	 */
	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = {Address.class,SingularPerson.class})
	@SessionFactory
	void testSingularNamingPattern(SessionFactoryScope sfScope) {
		final SessionFactoryImplementor sessionFactory = sfScope.getSessionFactory();
		final MappingMetamodelImplementor mappingMetamodel = sessionFactory.getMappingMetamodel();
		final EntityPersister persister = mappingMetamodel.getEntityDescriptor( SingularPerson.class );
		verifyColumnNames( persister.findAttributeMapping( "homeAddress" ), "home_" );

		final EmbeddedAttributeMapping workAddressMapping = persister
				.findAttributeMapping( "workAddress" )
				.asEmbeddedAttributeMapping();
		final EmbeddableMappingType workAddressType = workAddressMapping.getEmbeddableTypeDescriptor();

		final AttributeMapping workStreetMapping = workAddressType.findAttributeMapping( "street" );
		assertThat( workStreetMapping.getJdbcTypeCount() ).isEqualTo( 1 );
		assertThat( workStreetMapping.getSelectable( 0 ).getSelectionExpression() ).isEqualTo( "street" );

		final AttributeMapping workCityMapping = workAddressType.findAttributeMapping( "city" );
		assertThat( workCityMapping.getJdbcTypeCount() ).isEqualTo( 1 );
		assertThat( workCityMapping.getSelectable( 0 ).getSelectionExpression() ).isEqualTo( "city" );
	}

	/**
	 * Test use of {@code @EmbeddedColumnNaming} one 2 separate embedded attributes
	 */
	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = {Address.class,GoodPerson.class})
	@SessionFactory
	void testGoodNamingPattern(SessionFactoryScope sfScope) {
		final SessionFactoryImplementor sessionFactory = sfScope.getSessionFactory();
		final MappingMetamodelImplementor mappingMetamodel = sessionFactory.getMappingMetamodel();
		final EntityPersister persister = mappingMetamodel.getEntityDescriptor( GoodPerson.class );
		verifyColumnNames( persister.findAttributeMapping( "homeAddress" ), "home_" );
		verifyColumnNames( persister.findAttributeMapping( "workAddress" ), "work_" );
	}

	/**
	 * Test {@code @EmbeddedColumnNaming} with more than 1 format marker
	 */
	@Test
	@ServiceRegistry
	void testBadNamingPattern1(ServiceRegistryScope registryScope) {
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( Address.class, BadPatternPerson1.class );

		try {
			metadataSources.buildMetadata();
			fail( "Expecting an exception" );
		}
		catch (MappingException expected) {
			// expected outcome
			assertThat( expected.getMessage() )
					.startsWith( "@EmbeddedColumnNaming expects pattern with exactly 1 format maker, but found 2 - `%s_home_%s`" );
			assertThat( expected.getMessage() )
					.endsWith( "BadPatternPerson1#homeAddress)" );
		}
	}

	/**
	 * Test {@code @EmbeddedColumnNaming} with 0 format markers
	 */
	@Test
	@ServiceRegistry
	void testBadNamingPattern2(ServiceRegistryScope registryScope) {
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( Address.class, BadPatternPerson2.class );

		try {
			metadataSources.buildMetadata();
			fail( "Expecting an exception" );
		}
		catch (MappingException expected) {
			// expected outcome
			assertThat( expected.getMessage() )
					.startsWith( "@EmbeddedColumnNaming expects pattern with exactly 1 format maker, but found 0 - `home`" );
			assertThat( expected.getMessage() )
					.endsWith( "BadPatternPerson2#homeAddress)" );
		}
	}

	@Embeddable
	public static class Address {
		private String street;
		private String city;
		private String state;
		private ZipPlus zip;
	}

	@Embeddable
	public static class ZipPlus {
		private String code;
		private String plus4;
	}

	@Entity(name="BaselinePerson")
	@Table(name="baseline_person")
	public static class BaselinePerson {
		@Id
		private Integer id;
		private String name;

		@Embedded
		@EmbeddedColumnNaming("home_%s")
		private Address homeAddress;
	}

	@Entity(name="SingularPerson")
	@Table(name="singular_person")
	public static class SingularPerson {
		@Id
		private Integer id;
		private String name;

		@Embedded
		@EmbeddedColumnNaming("home_%s")
		private Address homeAddress;

		@Embedded
		private Address workAddress;
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

	@Embeddable
	public static class PhoneNumber {
		private String countryCode;
		private String areaCode;
		private String prefix;
		private String lineNumber;
	}

	@Entity(name="Company")
	@Table(name="companies")
	public static class Company {
		@Id
		private Integer id;
		private String name;

	}
}
