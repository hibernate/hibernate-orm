/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.basics.sql;

import java.sql.Clob;
import java.sql.Types;
import java.util.function.Consumer;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.SqlType;
import org.hibernate.annotations.SqlTypeCode;
import org.hibernate.annotations.SqlTypeRegistration;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.ClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.NClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.NVarcharTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.TinyIntTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = SqlTypeTests.SimpleEntity.class
)
public class SqlTypeTests {

	@Test
	public void verifyResolutions(DomainModelScope scope) {
		final PersistentClass entityBinding = scope.getDomainModel().getEntityBinding( SimpleEntity.class.getName() );
		final boolean supportsNationalizedTypes = scope.getDomainModel()
				.getDatabase()
				.getDialect()
				.supportsNationalizedTypes();
		verifyResolution( entityBinding.getProperty( "materializedClob" ), ClobTypeDescriptor.class );
		verifyResolution( entityBinding.getProperty( "materializedNClob" ), NClobTypeDescriptor.class );

		verifyResolution( entityBinding.getProperty( "jpaMaterializedClob" ), ClobTypeDescriptor.class );
		verifyResolution(
				entityBinding.getProperty( "jpaMaterializedNClob" ),
				supportsNationalizedTypes ? NClobTypeDescriptor.class : ClobTypeDescriptor.class
		);

		verifyResolution(
				entityBinding.getProperty( "nationalizedString" ),
				supportsNationalizedTypes ? NVarcharTypeDescriptor.class : VarcharTypeDescriptor.class
		);
		verifyResolution(
				entityBinding.getProperty( "nationalizedClob" ),
				supportsNationalizedTypes ? NClobTypeDescriptor.class : ClobTypeDescriptor.class
		);

		verifyResolution( entityBinding.getProperty( "customType" ), CustomSqlTypeDescriptor.class );
		verifyResolution( entityBinding.getProperty( "customTypeRegistration" ), RegisteredCustomSqlTypeDescriptor.class );
	}

	private void verifyResolution(Property property, Class<? extends SqlTypeDescriptor> expectedSqlTypeDescriptor) {
		verifyResolution(
				property,
				sqlTypeDescriptor -> {
					assertThat( "For property `" + property.getName() + "`", sqlTypeDescriptor, instanceOf( expectedSqlTypeDescriptor ) );
				}
		);
	}

	private void verifyResolution(
			Property property,
			Consumer<SqlTypeDescriptor> stdVerifier) {
		final Value value = property.getValue();
		assertThat( value, instanceOf( BasicValue.class ) );
		final BasicValue basicValue = (BasicValue) value;
		final BasicValue.Resolution<?> resolution = basicValue.resolve();

		stdVerifier.accept( resolution.getRelationalSqlTypeDescriptor() );
	}

	@Entity( name = "SimpleEntity" )
	@Table( name = "simple_entity" )
	@SqlTypeRegistration( value = RegisteredCustomSqlTypeDescriptor.class, registrationCode = Integer.MAX_VALUE - 1 )
	@SuppressWarnings("unused")
	public static class SimpleEntity {
		@Id
		private Integer id;

		@SqlTypeCode( Types.CLOB )
		private String materializedClob;

		@SqlTypeCode( Types.NCLOB )
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

		@SqlType( CustomSqlTypeDescriptor.class )
		private Integer customType;

		@SqlTypeCode( Integer.MAX_VALUE - 1 )
		private Integer customTypeRegistration;
	}

	public static class CustomSqlTypeDescriptor implements SqlTypeDescriptor {
		@Override
		public int getSqlType() {
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

	public static class RegisteredCustomSqlTypeDescriptor extends CustomSqlTypeDescriptor {
	}
}
