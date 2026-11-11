package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.BigDecimalJavaType;
import org.hibernate.type.descriptor.java.CharacterJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.LocalTimeJavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.java.spi.UnknownBasicJavaType;
import org.hibernate.type.descriptor.jdbc.DecimalJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.dialect.type.internal.OracleLocalTimeJdbcType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.VARCHAR;

/// Cast sizes describe the relational Java value, without invoking its converter.
///
/// @author Steve Ebersole
class ConvertedCastSizingTests {
	private final OracleDialect dialect = new OracleDialect();
	private final TypeConfiguration types = new TypeConfiguration();

	@Test
	void temporalDefaultAndExplicitPrecision() {
		final var ddl = new DdlTypeImpl( TIMESTAMP, "timestamp($p)", dialect );
		final var converted = converted( new UnknownBasicJavaType<>( Holder.class ), LocalTimeJavaType.INSTANCE,
				OracleLocalTimeJdbcType.INSTANCE );
		assertThat( ddl.getCastTypeName( Size.nil(), converted, types.getDdlTypeRegistry() ) ).isEqualTo( "timestamp(0)" );
		assertThat( ddl.getCastTypeName( Size.nil().setPrecision( 9 ), converted, types.getDdlTypeRegistry() ) ).isEqualTo( "timestamp(9)" );
		assertThat( ddl.getNarrowCastTypeName( Size.nil(), converted, types.getDdlTypeRegistry() ) ).isEqualTo( "timestamp(0)" );
	}

	@Test
	void characterSpecialCaseUsesTheRelationalType() {
		final var ddl = new DdlTypeImpl( VARCHAR, "varchar2($l)", dialect );
		final var toCharacter = converted( new UnknownBasicJavaType<>( Holder.class ), CharacterJavaType.INSTANCE, VarcharJdbcType.INSTANCE );
		assertThat( ddl.getCastTypeName( Size.nil(), toCharacter, types.getDdlTypeRegistry() ) ).isEqualTo( "varchar2(1)" );
		final var toString = converted( CharacterJavaType.INSTANCE, StringJavaType.INSTANCE, VarcharJdbcType.INSTANCE );
		assertThat( ddl.getCastTypeName( Size.nil(), toString, types.getDdlTypeRegistry() ) ).isEqualTo(
				ddl.getCastTypeName( Size.nil(), new BasicTypeImpl<>( StringJavaType.INSTANCE, VarcharJdbcType.INSTANCE ), types.getDdlTypeRegistry() ) );
		assertThat( ddl.getCastTypeName( Size.length( 12 ), toCharacter, types.getDdlTypeRegistry() ) ).isEqualTo( "varchar2(12)" );
	}

	@Test
	void numericAndDistinctNarrowDefaultsUseTheRelationalType() {
		final var ddl = new DdlTypeImpl( DECIMAL, "number($p,$s)", "number($p,$s)", "number($p,$s)", "narrow($p,$s)", dialect );
		final var converted = converted( new UnknownBasicJavaType<>( Holder.class ), BigDecimalJavaType.INSTANCE, DecimalJdbcType.INSTANCE );
		final var plain = new BasicTypeImpl<>( BigDecimalJavaType.INSTANCE, DecimalJdbcType.INSTANCE );
		assertThat( ddl.getCastTypeName( Size.nil(), converted, types.getDdlTypeRegistry() ) ).isEqualTo(
				ddl.getCastTypeName( Size.nil(), plain, types.getDdlTypeRegistry() ) );
		assertThat( ddl.getNarrowCastTypeName( Size.nil(), converted, types.getDdlTypeRegistry() ) ).isEqualTo(
				ddl.getNarrowCastTypeName( Size.nil(), plain, types.getDdlTypeRegistry() ) );
		assertThat( ddl.getCastTypeName( Size.precision( 12, 4 ), converted, types.getDdlTypeRegistry() ) ).isEqualTo( "number(12,4)" );
	}

	private static <D, R> ConvertedBasicTypeImpl<D> converted(JavaType<D> domain, JavaType<R> relational, JdbcType jdbc) {
		return new ConvertedBasicTypeImpl<>( "cast-test", jdbc, new BasicValueConverter<D, R>() {
			@Override
			public D toDomainValue(R value) {
				throw new AssertionError( "Sizing must not convert values" );
			}
			@Override
			public R toRelationalValue(D value) {
				throw new AssertionError( "Sizing must not convert values" );
			}
			@Override
			public JavaType<D> getDomainJavaType() {
				return domain;
			}
			@Override
			public JavaType<R> getRelationalJavaType() {
				return relational;
			}
		} );
	}

	record Holder(Object value) {}
}
