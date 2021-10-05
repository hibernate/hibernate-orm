/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.basics.sql;

import java.sql.Types;
import java.util.Map;
import java.util.function.Consumer;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.MapKeyJdbcType;
import org.hibernate.annotations.MapKeyJdbcTypeCode;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TinyIntJdbcTypeDescriptor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = MapKeyJdbcTypeTests.MyEntity.class )
@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "The jTDS driver does support NVARCHAR so we remap it to CLOB")
public class MapKeyJdbcTypeTests {

	@Test
	public void verifyResolutions(DomainModelScope scope) {
		final Dialect dialect = scope.getDomainModel().getDatabase().getDialect();
		final NationalizationSupport nationalizationSupport = dialect.getNationalizationSupport();

		final PersistentClass entityBinding = scope.getDomainModel().getEntityBinding( MyEntity.class.getName() );

		verifyJdbcTypeCodes(
				entityBinding.getProperty( "baseMap" ),
				Types.INTEGER,
				Types.VARCHAR
		);

		verifyJdbcTypeCodes(
				entityBinding.getProperty( "sqlTypeCodeMap" ),
				Types.TINYINT,
				nationalizationSupport.getVarcharVariantCode()
		);

		verifyJdbcTypeCodes(
				entityBinding.getProperty( "sqlTypeMap" ),
				Types.TINYINT,
				nationalizationSupport.getVarcharVariantCode()
		);

	}

	private void verifyJdbcTypeCodes(Property property, int keyJdbcTypeCode, int valueJdbcTypeCode) {
		verifyJdbcTypeResolution(
				property,
				(keyJdbcType) -> assertThat(
						"Map key for `" + property.getName() + "`",
						keyJdbcType.getJdbcTypeCode(),
						equalTo( keyJdbcTypeCode )
				),
				(valueJdbcType) -> assertThat(
						"Map value for `" + property.getName() + "`",
						valueJdbcType.getJdbcTypeCode(),
						equalTo( valueJdbcTypeCode )
				)
		);
	}

	private void verifyJdbcTypeResolution(
			Property property,
			Consumer<JdbcTypeDescriptor> keyTypeVerifier,
			Consumer<JdbcTypeDescriptor> valueTypeVerifier) {
		assertThat( property.getValue(), instanceOf( org.hibernate.mapping.Map.class ) );
		final org.hibernate.mapping.Map mapValue = (org.hibernate.mapping.Map) property.getValue();

		assertThat( mapValue.getIndex(), instanceOf( BasicValue.class ) );
		final BasicValue indexValue = (BasicValue) mapValue.getIndex();
		final BasicValue.Resolution<?> indexResolution = indexValue.resolve();
		keyTypeVerifier.accept( indexResolution.getJdbcTypeDescriptor() );

		assertThat( mapValue.getElement(), instanceOf( BasicValue.class ) );
		final BasicValue elementValue = (BasicValue) mapValue.getElement();
		final BasicValue.Resolution<?> elementResolution = elementValue.resolve();
		valueTypeVerifier.accept( elementResolution.getJdbcTypeDescriptor() );
	}

	@Entity( name = "MyEntity" )
	@Table( name = "my_entity" )
	public static class MyEntity {
		@Id
		private Integer id;

		@ElementCollection
		private Map<Integer,String> baseMap;

		@ElementCollection
		@JdbcTypeCode( Types.NVARCHAR )
		@MapKeyJdbcTypeCode( @JdbcTypeCode( Types.TINYINT ) )
		private Map<Integer,String> sqlTypeCodeMap;

		@ElementCollection
		@JdbcTypeCode( Types.NVARCHAR )
		@MapKeyJdbcType( @JdbcType( TinyIntJdbcTypeDescriptor.class ) )
		private Map<Integer,String> sqlTypeMap;
	}
}
