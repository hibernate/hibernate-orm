/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.basics.sql;

import java.sql.Clob;
import java.sql.Types;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.JdbcTypeRegistration;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.ClobTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.NClobTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.NVarcharTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TinyIntTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.VarcharTypeDescriptor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = JdbcTypeTests.SimpleEntity.class
)
public class JdbcTypeTests {

	@Test
	public void verifyResolutions(DomainModelScope scope) {
		final Dialect dialect = scope.getDomainModel()
				.getDatabase()
				.getDialect();
		final NationalizationSupport nationalizationSupport = dialect.getNationalizationSupport();

		final PersistentClass entityBinding = scope.getDomainModel().getEntityBinding( SimpleEntity.class.getName() );

		verifyJdbcTypeCode( entityBinding.getProperty( "materializedClob" ), Types.CLOB );
		verifyJdbcTypeCode( entityBinding.getProperty( "materializedNClob" ), nationalizationSupport.getClobVariantCode() );
		verifyJdbcTypeCode( entityBinding.getProperty( "jpaMaterializedClob" ), Types.CLOB );
		verifyJdbcTypeCode( entityBinding.getProperty( "jpaMaterializedNClob" ), nationalizationSupport.getClobVariantCode() );

		verifyJdbcTypeCode( entityBinding.getProperty( "nationalizedString" ), nationalizationSupport.getVarcharVariantCode() );
		verifyJdbcTypeCode( entityBinding.getProperty( "nationalizedClob" ), nationalizationSupport.getClobVariantCode() );

		verifyResolution( entityBinding.getProperty( "customType" ), CustomJdbcTypeDescriptor.class );
		verifyResolution( entityBinding.getProperty( "customTypeRegistration" ), RegisteredCustomJdbcTypeDescriptor.class );
	}

	private void verifyResolution(Property property, Class<? extends JdbcTypeDescriptor> expectedSqlTypeDescriptor) {
		verifyResolution(
				property,
				sqlTypeDescriptor -> {
					assertThat( "For property `" + property.getName() + "`", sqlTypeDescriptor, instanceOf( expectedSqlTypeDescriptor ) );
				}
		);
	}

	private void verifyJdbcTypeCode(Property property, int typeCode) {
		verifyJdbcTypeResolution(
				property,
				(p, jdbcType) -> assertThat(
						"JDBC type code mismatch for `" + property.getName() + "`",
						jdbcType.getJdbcTypeCode(),
						equalTo( typeCode )
				)
		);
	}

	private void verifyJdbcTypeResolution(
			Property property,
			BiConsumer<Property, JdbcTypeDescriptor> verifier) {
		final Value value = property.getValue();
		assertThat( value, instanceOf( BasicValue.class ) );
		final BasicValue basicValue = (BasicValue) value;
		final BasicValue.Resolution<?> resolution = basicValue.resolve();

		verifier.accept( property, resolution.getJdbcTypeDescriptor() );
	}


	private void verifyResolution(
			Property property,
			Consumer<JdbcTypeDescriptor> stdVerifier) {
		final Value value = property.getValue();
		assertThat( value, instanceOf( BasicValue.class ) );
		final BasicValue basicValue = (BasicValue) value;
		final BasicValue.Resolution<?> resolution = basicValue.resolve();

		stdVerifier.accept( resolution.getJdbcTypeDescriptor() );
	}

	@Entity( name = "SimpleEntity" )
	@Table( name = "simple_entity" )
	@JdbcTypeRegistration( value = RegisteredCustomJdbcTypeDescriptor.class, registrationCode = Integer.MAX_VALUE - 1 )
	@SuppressWarnings("unused")
	public static class SimpleEntity {
		@Id
		private Integer id;

		@JdbcTypeCode( Types.CLOB )
		private String materializedClob;

		@JdbcTypeCode( Types.NCLOB )
		private String materializedNClob;

		@Lob
		private String jpaMaterializedClob;

		@Lob
		@Nationalized
		private String jpaMaterializedNClob;

		@Nationalized
		private String nationalizedString;

		@Nationalized
		private Clob nationalizedClob;

		@JdbcType( CustomJdbcTypeDescriptor.class )
		private Integer customType;

		@JdbcTypeCode( Integer.MAX_VALUE - 1 )
		private Integer customTypeRegistration;
	}

	public static class CustomJdbcTypeDescriptor implements JdbcTypeDescriptor {
		@Override
		public int getJdbcTypeCode() {
			return Types.TINYINT;
		}

		@Override
		public boolean canBeRemapped() {
			return false;
		}

		@Override
		public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
			return TinyIntTypeDescriptor.INSTANCE.getBinder( javaTypeDescriptor );
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
			return TinyIntTypeDescriptor.INSTANCE.getExtractor( javaTypeDescriptor );
		}
	}

	public static class RegisteredCustomJdbcTypeDescriptor extends CustomJdbcTypeDescriptor {
	}
}
