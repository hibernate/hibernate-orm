/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.TruthValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Value;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.schema.extract.internal.ColumnInformationImpl;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hibernate.tool.schema.internal.ColumnDefinitions.extractType;
import static org.hibernate.tool.schema.internal.ColumnDefinitions.extractTypeName;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Yanming Zhou
 */
public class ColumnDefinitionsTest {

	@Test
	public void testExtractType() {
		assertThat(extractType("varchar(255)"), is("varchar(255)"));
		assertThat(extractType("varchar(255)  primary key"), is("varchar(255)"));
		assertThat(extractType("varchar(255)  unique key"), is("varchar(255)"));
		assertThat(extractType("varchar(255)  null"), is("varchar(255)"));
		assertThat(extractType("varchar(255)  not null"), is("varchar(255)"));
		assertThat(extractType("varchar(255)  generated as 'test' not null"), is("varchar(255)"));
		assertThat(extractType("varchar(255)  default 'test' not null"), is("varchar(255)"));
		assertThat(extractType("varchar(255)  comment 'test'"), is("varchar(255)"));
		assertThat(extractType("varchar(255)  check ()"), is("varchar(255)"));
	}

	@Test
	public void testExtractTypeName() {
		assertThat(extractTypeName("INT(10) NOT NULL"), is("integer"));
		assertThat(extractTypeName("INT(10) UNSIGNED NOT NULL"), is("integer unsigned"));
	}

    @Test
    public void matchIntegerType() {
		assertHasMatchingType("integer", SqlTypes.INTEGER, "integer", SqlTypes.INTEGER, true);
		assertHasMatchingType("integer not null", SqlTypes.INTEGER, "integer", SqlTypes.INTEGER, true);
		assertHasMatchingType("integer default 0", SqlTypes.INTEGER, "integer", SqlTypes.INTEGER, true);
		assertHasMatchingType("integer not null default 0", SqlTypes.INTEGER, "integer", SqlTypes.INTEGER, true);
		assertHasMatchingType("integer", SqlTypes.INTEGER, "int", SqlTypes.INTEGER, true);
		assertHasMatchingType("int", SqlTypes.INTEGER, "integer", SqlTypes.INTEGER, true);
		assertHasMatchingType("integer", SqlTypes.INTEGER, "bigint", SqlTypes.BIGINT, false);
		assertHasMatchingType("bigint", SqlTypes.BIGINT, "integer", SqlTypes.INTEGER, false);
	}

	@Test
	public void matchDecimalType() {
		assertHasMatchingType("decimal(10,2)", SqlTypes.DECIMAL, "decimal", SqlTypes.DECIMAL, true);
		assertHasMatchingType("decimal( 10 , 2 )", SqlTypes.DECIMAL, "decimal", SqlTypes.DECIMAL, true);
	}

	@Test
	public void matchComplexCharacterVaryingType() {
		assertHasMatchingType("character varying(255) not null default '-'", SqlTypes.VARCHAR, "varchar", SqlTypes.VARCHAR, true);
	}

	@Test
	public void shouldNotMatchIfColumnDefinitionNotEqualsToActualTypeNameButInferredTypeCodeEqualsToActualTypeCode() {
		assertHasMatchingType("integer not null default 0", SqlTypes.BIGINT, "bigint", SqlTypes.BIGINT, false);
	}

	private void assertHasMatchingType(String columnDefinition, int inferredTypeCode, String actualTypeName, int actualTypeCode, boolean matching) {
		Column column = new Column();
		Value value = mock();
		given(value.getType()).willReturn(JavaObjectType.INSTANCE);
		column.setValue(value);
		column.setColumnDefinition(columnDefinition);
		column.setSqlType(columnDefinition);
		column.setSqlTypeCode(inferredTypeCode);

		ColumnInformation columnInformation = new ColumnInformationImpl(
				null,
				null,
				actualTypeCode,
				actualTypeName,
				255,
				0,
				TruthValue.TRUE
		);

		Metadata metadata = new MetadataSources( ServiceRegistryUtil.serviceRegistry() ).buildMetadata();
		assertThat(ColumnDefinitions.hasMatchingType(column, columnInformation, metadata, metadata.getDatabase().getDialect()),
				is(matching));
	}

}