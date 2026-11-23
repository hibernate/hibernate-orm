package org.hibernate.orm.test.mapping.embeddable;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.aggregate.internal.OracleAggregateSupport;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.SqlTypedMappingImpl;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.XmlJdbcType;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Null guards must compose paths without duplicating guarded parent expressions.
/// These SQL checks cover synthetic server profiles, not live Oracle19 execution.
///
/// @author Steve Ebersole
class OracleXmlNullExtractionSqlTests {
	private static final BasicTypeImpl<String> XML = new BasicTypeImpl<>( StringJavaType.INSTANCE, XmlJdbcType.INSTANCE );

	@ParameterizedTest
	@ValueSource(ints = { 19, 21, 23 })
	void guardedAggregatesComposeAgainstTheOriginalDocument(int version) {
		final String first = read( version, SqlTypes.SQLXML, "doc", "middle", XML );
		assertThat( first ).isEqualTo( guarded( "/e/middle/*" ) );
		final String second = read( version, SqlTypes.SQLXML, first, "leaf", XML );
		assertThat( second ).isEqualTo( guarded( "/e/middle/leaf/*" ) );
		final String third = read( version, SqlTypes.SQLXML, second, "deeper", XML );
		assertThat( third ).isEqualTo( guarded( "/e/middle/leaf/deeper/*" ) );
	}

	@ParameterizedTest
	@ValueSource(ints = { 19, 21, 23 })
	void existingUnguardedAndArrayParentsStillCompose(int version) {
		final String oldParent = "xmlelement(\"e\",xmlquery('/e/middle/*' passing doc returning content))";
		assertThat( read( version, SqlTypes.SQLXML, oldParent, "leaf", XML ) )
				.isEqualTo( guarded( "/e/middle/leaf/*" ) );
		final String arrayParent = "xmlquery('/e/items' passing doc returning content)";
		assertThat( read( version, SqlTypes.XML_ARRAY, arrayParent, "leaf", XML ) )
				.isEqualTo( guarded( "/e/items/leaf/*" ) );
		final var arrayType = mock( JdbcType.class );
		when( arrayType.getDefaultSqlTypeCode() ).thenReturn( SqlTypes.XML_ARRAY );
		final var array = new BasicTypeImpl<>( StringJavaType.INSTANCE, arrayType );
		assertThat( read( version, SqlTypes.SQLXML, "doc", "items", array ) )
				.isEqualTo( "xmlelement(\"Collection\",xmlquery('/e/items/*' passing doc returning content))" );
	}

	private static String read(int version, int format, String parent, String member, JdbcMapping mapping) {
		final var support = (OracleAggregateSupport) new OracleDialect( DatabaseVersion.make( version ) ).getAggregateSupport();
		return support.aggregateComponentCustomReadExpression(
				"?", "?", parent, member, format, new SqlTypedMappingImpl( mapping ), new TypeConfiguration() );
	}

	private static String guarded(String path) {
		final String child = "xmlquery('" + path + "' passing doc returning content)";
		return "case when " + child + " is null then null else xmlelement(\"e\"," + child + ") end";
	}
}
