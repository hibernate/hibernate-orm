/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.basics.sql;

import java.sql.Types;
import java.util.Map;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.MapKeyJdbcType;
import org.hibernate.annotations.MapKeyJdbcTypeCode;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.descriptor.jdbc.IntegerTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.NVarcharTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TinyIntTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.VarcharTypeDescriptor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = MapKeyJdbcTypeTests.MyEntity.class )
public class MapKeyJdbcTypeTests {

	@Test
	public void verifyResolutions(DomainModelScope scope) {
		final PersistentClass entityBinding = scope.getDomainModel().getEntityBinding( MyEntity.class.getName() );

		verifyResolutions( entityBinding.getProperty( "baseMap" ), IntegerTypeDescriptor.class, VarcharTypeDescriptor.class );
		verifyResolutions( entityBinding.getProperty( "sqlTypeCodeMap" ), TinyIntTypeDescriptor.class, NVarcharTypeDescriptor.class );
		verifyResolutions( entityBinding.getProperty( "sqlTypeMap" ), TinyIntTypeDescriptor.class, NVarcharTypeDescriptor.class );

	}

	private void verifyResolutions(
			Property property,
			Class<? extends JdbcTypeDescriptor> keyStd,
			Class<? extends JdbcTypeDescriptor> valueStd) {
		assertThat( property.getValue(), instanceOf( org.hibernate.mapping.Map.class ) );

		final org.hibernate.mapping.Map mapValue = (org.hibernate.mapping.Map) property.getValue();

		assertThat( mapValue.getIndex(), instanceOf( BasicValue.class ) );
		final BasicValue indexValue = (BasicValue) mapValue.getIndex();
		final BasicValue.Resolution<?> indexResolution = indexValue.resolve();
		assertThat( indexResolution.getJdbcTypeDescriptor(), instanceOf( keyStd ) );
		assertThat( indexResolution.getJdbcMapping().getJdbcTypeDescriptor(), instanceOf( keyStd ) );
		assertThat( indexResolution.getLegacyResolvedBasicType().getJdbcTypeDescriptor(), instanceOf( keyStd ) );

		assertThat( mapValue.getElement(), instanceOf( BasicValue.class ) );
		final BasicValue elementValue = (BasicValue) mapValue.getElement();
		final BasicValue.Resolution<?> elementResolution = elementValue.resolve();
		assertThat( elementResolution.getJdbcTypeDescriptor(), instanceOf( valueStd ) );
		assertThat( elementResolution.getJdbcMapping().getJdbcTypeDescriptor(), instanceOf( valueStd ) );
		assertThat( elementResolution.getLegacyResolvedBasicType().getJdbcTypeDescriptor(), instanceOf( valueStd ) );
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
		@MapKeyJdbcType( @JdbcType( TinyIntTypeDescriptor.class ) )
		private Map<Integer,String> sqlTypeMap;
	}
}
