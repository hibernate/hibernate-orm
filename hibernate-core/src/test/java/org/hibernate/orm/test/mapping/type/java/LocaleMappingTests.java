/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicValuedCollectionPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = LocaleMappingTests.LocaleMappingTestEntity.class)
@SessionFactory
@JiraKey("HHH-17466")
public class LocaleMappingTests {

	@Test
	public void basicAssertions(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final JdbcTypeRegistry jdbcTypeRegistry = sessionFactory.getTypeConfiguration().getJdbcTypeRegistry();
		final EntityPersister entityDescriptor = sessionFactory.getMappingMetamodel().getEntityDescriptor(
				LocaleMappingTestEntity.class );

		{
			final BasicAttributeMapping localeAttribute = (BasicAttributeMapping) entityDescriptor
					.findAttributeMapping("locale" );
			assertThat( localeAttribute.getJdbcMapping().getJdbcType() )
					.isEqualTo( jdbcTypeRegistry.getDescriptor( Types.VARCHAR ) );
			assertThat( localeAttribute.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass() )
					.isEqualTo( Locale.class );
		}

		{
			final PluralAttributeMapping localesAttribute = (PluralAttributeMapping) entityDescriptor
					.findAttributeMapping( "locales" );
			final BasicValuedCollectionPart elementDescriptor = (BasicValuedCollectionPart) localesAttribute.getElementDescriptor();
			assertThat( elementDescriptor.getJdbcMapping().getJdbcType() )
					.isEqualTo( jdbcTypeRegistry.getDescriptor( Types.VARCHAR ) );
			assertThat( elementDescriptor.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass() )
					.isEqualTo( Locale.class );
		}

		{
			final PluralAttributeMapping countByLocaleAttribute = (PluralAttributeMapping) entityDescriptor
					.findAttributeMapping("countByLocale" );
			final BasicValuedCollectionPart keyDescriptor = (BasicValuedCollectionPart) countByLocaleAttribute.getIndexDescriptor();
			assertThat( keyDescriptor.getJdbcMapping().getJdbcType() )
					.isEqualTo( jdbcTypeRegistry.getDescriptor( Types.VARCHAR ) );
			assertThat( keyDescriptor.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass() )
					.isEqualTo( Locale.class );
		}
	}

	@Test
	public void testUsage(SessionFactoryScope scope) {
		final LocaleMappingTestEntity entity = new LocaleMappingTestEntity( 1, Locale.ENGLISH, "Hello" );
		final LocaleMappingTestEntity entity2 = new LocaleMappingTestEntity( 2, Locale.FRENCH, "Salut" );

		scope.inTransaction( (session) -> {
			session.persist( entity );
			session.persist( entity2 );
		} );

		try {
			scope.inTransaction( (session) -> assertThat( session.createQuery(
							"from LocaleMappingTestEntity where locale = ?1",
							LocaleMappingTestEntity.class
					)
					.setParameter( 1, Locale.FRENCH )
					.list() )
					.extracting( LocaleMappingTestEntity::getId )
					.containsExactly( 2 ) );
		}
		finally {
			scope.inTransaction( session -> session.remove( entity ) );
			scope.inTransaction( session -> session.remove( entity2 ) );
		}
	}

	@Entity(name = "LocaleMappingTestEntity")
	@Table(name = "locale_map_test_entity")
	public static class LocaleMappingTestEntity {
		private Integer id;
		private Locale locale;
		private String name;

		private Set<Locale> locales = new HashSet<>();
		private Map<Locale, Integer> countByLocale = new HashMap<>();

		public LocaleMappingTestEntity() {
		}

		public LocaleMappingTestEntity(Integer id, Locale locale, String name) {
			this.id = id;
			this.locale = locale;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Locale getLocale() {
			return locale;
		}

		public void setLocale(Locale locale) {
			this.locale = locale;
		}

		@ElementCollection
		@CollectionTable(
				name = "entity_locale",
				joinColumns = @JoinColumn(name = "entity_id")
		)
		@Column(name = "locales")
		public Set<Locale> getLocales() {
			return locales;
		}

		public void setLocales(Set<Locale> locales) {
			this.locales = locales;
		}

		@ElementCollection
		@CollectionTable(name = "count_by_locale", joinColumns = @JoinColumn(name = "entity_id"))
		@MapKeyColumn(name = "locl")
		@Column(name = "cnt")
		public Map<Locale, Integer> getCountByLocale() {
			return countByLocale;
		}

		public void setCountByLocale(Map<Locale, Integer> countByLocale) {
			this.countByLocale = countByLocale;
		}
	}
}
