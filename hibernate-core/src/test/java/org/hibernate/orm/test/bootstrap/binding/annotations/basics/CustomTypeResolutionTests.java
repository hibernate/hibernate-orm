/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.basics;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.UrlTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.CharTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
import org.hibernate.type.descriptor.jdbc.VarcharTypeDescriptor;
import org.hibernate.usertype.UserType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Resolution for custom-types
 *
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = @Setting( name = AvailableSettings.HBM2DDL_AUTO, value = "create-drop" )
)
@DomainModel( annotatedClasses = CustomTypeResolutionTests.Person.class )
public class CustomTypeResolutionTests {

	@Test
	public void testMappings(DomainModelScope scope) {
		final PersistentClass entityBinding = scope.getDomainModel().getEntityBinding( Person.class.getName() );

		final Property genderProperty = entityBinding.getProperty( "gender" );
		final BasicValue genderValue = (BasicValue) genderProperty.getValue();
		final BasicValue.Resolution<?> genderValueResolution = genderValue.resolve();
		assertThat( genderValueResolution.getLegacyResolvedBasicType(), instanceOf( GenderType.class ) );

		final Property string1Property = entityBinding.getProperty( "string1" );
		final BasicValue string1Value = (BasicValue) string1Property.getValue();
		final BasicValue.Resolution<?> string1ValueResolution = string1Value.resolve();
		assertThat( string1ValueResolution.getLegacyResolvedBasicType(), instanceOf( CustomType.class ) );
		assertThat( string1ValueResolution.getJdbcMapping(), instanceOf( CustomType.class ) );
		assertThat( string1ValueResolution.getLegacyResolvedBasicType(), is( string1ValueResolution.getJdbcMapping() ) );
		final CustomType string1TypeWrapper = (CustomType) string1ValueResolution.getJdbcMapping();
		assertThat( string1TypeWrapper.getUserType(), instanceOf( UserTypeImpl.class ) );

		final Property string2Property = entityBinding.getProperty( "string2" );
		final BasicValue string2Value = (BasicValue) string2Property.getValue();
		final BasicValue.Resolution<?> string2ValueResolution = string2Value.resolve();
		assertThat( string2ValueResolution.getLegacyResolvedBasicType(), instanceOf( CustomType.class ) );
		assertThat( string2ValueResolution.getJdbcMapping(), instanceOf( CustomType.class ) );
		assertThat( string2ValueResolution.getLegacyResolvedBasicType(), is( string2ValueResolution.getJdbcMapping() ) );
		final CustomType string2TypeWrapper = (CustomType) string2ValueResolution.getJdbcMapping();
		assertThat( string2TypeWrapper.getUserType(), instanceOf( UserTypeImpl.class ) );

		final Property url1Property = entityBinding.getProperty( "url1" );
		final BasicValue url1Value = (BasicValue) url1Property.getValue();
		final BasicValue.Resolution<?> url1ValueResolution = url1Value.resolve();
		assertThat( url1ValueResolution.getLegacyResolvedBasicType(), instanceOf( CustomTypeImpl.class ) );

		final Property url2Property = entityBinding.getProperty( "url2" );
		final BasicValue url2Value = (BasicValue) url2Property.getValue();
		final BasicValue.Resolution<?> url2ValueResolution = url2Value.resolve();
		assertThat( url2ValueResolution.getLegacyResolvedBasicType(), instanceOf( CustomTypeImpl.class ) );
	}

	@Test
	public void simpleUsageSmokeTest(DomainModelScope scope) {
		try ( SessionFactory sessionFactory = scope.getDomainModel().buildSessionFactory()) {
			// set up test data
			sessionFactory.inTransaction(
					session -> {
						try {
							session.persist(
									new Person(
											1,
											Gender.MALE,
											"str1",
											"str2",
											new URL( "http://url1" ),
											new URL( "http://url2" )
									)
							);
						}
						catch (MalformedURLException e) {
							throw new RuntimeException( e );
						}
					}
			);

			// try to read it back
			try {
				sessionFactory.inTransaction(
						session -> {
							final Person loaded = session.byId( Person.class ).load( 1 );
							assertThat( loaded, notNullValue() );
							assertThat( loaded.gender, is( Gender.MALE ) );
							assertThat( loaded.string1, is( "str1" ) );
							assertThat( loaded.string2, is( "str2" ) );
							assertThat( loaded.url1.getHost(), is( "url1" ) );
							assertThat( loaded.url2.getHost(), is( "url2" ) );
						}
				);
			}
			finally {
				sessionFactory.inTransaction(
						session -> session.createQuery( "delete Person" ).executeUpdate()
				);
			}
		}
	}


	public enum Gender { MALE, FEMALE }

	@SuppressWarnings("unused")
	@Entity( name = "Person" )
	@Table( name = "persons" )
	@TypeDef( name = "user-type", typeClass = UserTypeImpl.class )
	@TypeDef( name = "custom-type", typeClass = CustomTypeImpl.class )
	@TypeDef( name = "gender-type", typeClass = GenderType.class, defaultForType = Gender.class )
	public static class Person {
		@Id
		private Integer id;

		private Gender gender;

		@Type( type = "org.hibernate.orm.test.bootstrap.binding.annotations.basics.CustomTypeResolutionTests$UserTypeImpl" )
		private String string1;

		@Type( type = "user-type" )
		private String string2;

		@Type( type = "org.hibernate.orm.test.bootstrap.binding.annotations.basics.CustomTypeResolutionTests$CustomTypeImpl" )
		private URL url1;

		@Type( type = "custom-type" )
		private URL url2;

		public Person() {
		}

		public Person(Integer id, Gender gender, String string1, String string2, URL url1, URL url2) {
			this.id = id;
			this.gender = gender;
			this.string1 = string1;
			this.string2 = string2;
			this.url1 = url1;
			this.url2 = url2;
		}
	}

	public static class UserTypeImpl implements UserType {

		@Override
		public int[] sqlTypes() {
			return new int[] { Types.VARCHAR };
		}

		@Override
		public Class<String> returnedClass() {
			return String.class;
		}

		@Override
		public boolean equals(Object x, Object y) throws HibernateException {
			return Objects.equals( x, y );
		}

		@Override
		public int hashCode(Object x) throws HibernateException {
			return Objects.hashCode( x );
		}

		@Override
		public Object nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
			final String value = rs.getString( position );
			return rs.wasNull() ? null : value;
		}

		@Override
		public void nullSafeSet(
				PreparedStatement st,
				Object value,
				int index,
				SharedSessionContractImplementor session) throws HibernateException, SQLException {
			if ( value == null ) {
				st.setNull( index, Types.VARCHAR );
			}
			else {
				st.setString( index, value.toString() );
			}
		}

		@Override
		public Object deepCopy(Object value) throws HibernateException {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Serializable disassemble(Object value) throws HibernateException {
			return (String) value;
		}

		@Override
		public Object assemble(Serializable cached, Object owner) throws HibernateException {
			return cached;
		}

		@Override
		public Object replace(Object original, Object target, Object owner) throws HibernateException {
			return original;
		}
	}

	public static class CustomTypeImpl extends AbstractSingleColumnStandardBasicType<URL> {

		public CustomTypeImpl() {
			super( VarcharTypeDescriptor.INSTANCE, UrlTypeDescriptor.INSTANCE );
		}

		@Override
		public String getName() {
			return "Custom URL mapper";
		}
	}

	public static class GenderJtd implements JavaTypeDescriptor<Gender> {
		/**
		 * Singleton access
		 */
		public static final GenderJtd INSTANCE = new GenderJtd();

		@Override
		public JdbcTypeDescriptor getRecommendedJdbcType(JdbcTypeDescriptorIndicators context) {
			return CharTypeDescriptor.INSTANCE;
		}

		@Override
		public Gender fromString(String string) {
			if ( StringHelper.isEmpty( string ) ) {
				return null;
			}

			if ( "M".equalsIgnoreCase( string ) ) {
				return Gender.MALE;
			}
			else if ( "F".equalsIgnoreCase( string ) ) {
				return Gender.FEMALE;
			}

			throw new IllegalArgumentException( "Unrecognized Gender code : " + string );
		}

		@Override
		@SuppressWarnings("unchecked")
		public <X> X unwrap(Gender value, Class<X> type, WrapperOptions options) {
			// only defines support for String conversion as part of unwrap
			if ( value == null ) {
				return null;
			}

			if ( type.isInstance( value ) ) {
				return (X) value;
			}

			if ( String.class.equals( type ) ) {
				return (X) (value == Gender.MALE ? "M" : "F");
			}

			throw new IllegalArgumentException();
		}

		@Override
		public <X> Gender wrap(X value, WrapperOptions options) {
			if ( value == null ) {
				return null;
			}

			if ( value instanceof String ) {
				if ( "F".equalsIgnoreCase( (String) value ) ) {
					return Gender.FEMALE;
				}

				if ( "M".equalsIgnoreCase( (String) value ) ) {
					return Gender.MALE;
				}
			}

			throw new IllegalArgumentException();
		}

		@Override
		public Class<Gender> getJavaTypeClass() {
			return null;
		}
	}

	public static class GenderType extends AbstractSingleColumnStandardBasicType<Gender> {

		public GenderType() {
			super( VarcharTypeDescriptor.INSTANCE, GenderJtd.INSTANCE );
		}

		@Override
		public String getName() {
			return "Custom URL mapper";
		}
	}
}
