/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import java.util.Comparator;
import java.util.Locale;

import jakarta.persistence.Column;
import org.hibernate.annotations.JavaType;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@DomainModel(annotatedClasses = { OverrideStandardJavaTypeTest.SampleEntity.class })
@SessionFactory
@JiraKey("HHH-16781")
public class OverrideStandardJavaTypeTest {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor( SampleEntity.class );

		final var languageJavaTypeDescriptor = entityDescriptor.findAttributeMapping( "language" )
				.getSingleJdbcMapping().getJavaTypeDescriptor();
		assertInstanceOf( LocaleAsLanguageTagType.class, languageJavaTypeDescriptor );
	}

	@Test
	public void validateJpa(SessionFactoryScope scope) {
		final var id = scope.fromTransaction(
				session -> {
					final var entity = new SampleEntity();
					entity.language = Locale.forLanguageTag( "en-Latn" );
					session.persist( entity );
					return entity.id;
				}
		);
		scope.inSession(
				session -> assertEquals(
						Locale.forLanguageTag( "en-Latn" ),
						session.find( SampleEntity.class, id ).language
				)
		);
	}

	@Test
	public void validateNative(SessionFactoryScope scope) {
		final var id = scope.fromTransaction(
				session -> {
					final var entity = new SampleEntity();
					entity.language = Locale.forLanguageTag( "en-Latn" );
					session.persist( entity );
					return entity.id;
				}
		);
		scope.inSession( session -> {
			String quotedLanguage = session.getDialect().toQuotedIdentifier( "language" );
			assertEquals(
					"en-Latn",
					session.createNativeQuery(
									"select " + quotedLanguage + " from locale_as_language_tag where id=:id", String.class )
							.setParameter( "id", id )
							.getSingleResult()
			);
		} );
	}

	@Entity
	@Table(name = "locale_as_language_tag")
	public static class SampleEntity {

		@Id
		@GeneratedValue
		private Integer id;

		@JavaType(LocaleAsLanguageTagType.class)
		// LANGUAGE is a SQL:2003 reserved words, so dialects with autoQuoteKeywords require it quoted,
		// so quote it always to simplify the test
		@Column(name = "`language`")
		private Locale language;
	}

	public static class LocaleAsLanguageTagType extends AbstractClassJavaType<Locale> {

		public static final LocaleAsLanguageTagType INSTANCE = new LocaleAsLanguageTagType();

		public static class LocaleComparator implements Comparator<Locale> {

			public static final LocaleComparator INSTANCE = new LocaleComparator();

			@Override
			public int compare(Locale o1, Locale o2) {
				return o1.toString().compareTo( o2.toString() );
			}
		}

		LocaleAsLanguageTagType() {
			super( Locale.class );
		}

		@Override
		public Comparator<Locale> getComparator() {
			return LocaleComparator.INSTANCE;
		}

		@Override
		public String toString(Locale value) {
			return value.toLanguageTag();
		}

		private Locale fromString(String string) {
			return Locale.forLanguageTag( string );
		}

		@Override
		@SuppressWarnings({ "unchecked" })
		public <X> X unwrap(Locale value, Class<X> type, WrapperOptions options) {
			if ( value == null ) {
				return null;
			}
			if ( String.class.isAssignableFrom( type ) ) {
				return (X) toString( value );
			}
			throw unknownUnwrap( type );
		}

		@Override
		public <X> Locale wrap(X value, WrapperOptions options) {
			if ( value == null ) {
				return null;
			}
			if ( value instanceof String ) {
				return fromString( (String) value );
			}
			throw unknownWrap( value.getClass() );
		}

		@Override
		public JdbcType getRecommendedJdbcType(final JdbcTypeIndicators indicators) {
			return VarcharJdbcType.INSTANCE;
		}
	}
}
