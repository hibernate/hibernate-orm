/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type;

import java.util.Comparator;
import java.util.Locale;

import org.hibernate.annotations.JavaType;
import org.hibernate.community.dialect.AltibaseDialect;
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
import org.hibernate.testing.orm.junit.SkipForDialect;
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
	@SkipForDialect( dialectClass = AltibaseDialect.class, reason = "'LANGUAGE' is not escaped even though autoQuoteKeywords is enabled")
	public void validateNative(SessionFactoryScope scope) {
		final var id = scope.fromTransaction(
				session -> {
					final var entity = new SampleEntity();
					entity.language = Locale.forLanguageTag( "en-Latn" );
					session.persist( entity );
					return entity.id;
				}
		);
		scope.inSession(
				session ->
						assertEquals(
								"en-Latn",
								session.createNativeQuery(
												"select language from locale_as_language_tag where id=:id", String.class )
										.setParameter( "id", id )
										.getSingleResult()
						)
		);
	}


	@Entity
	@Table(name = "locale_as_language_tag")
	public static class SampleEntity {

		@Id
		@GeneratedValue
		private Integer id;

		@JavaType(LocaleAsLanguageTagType.class)
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
