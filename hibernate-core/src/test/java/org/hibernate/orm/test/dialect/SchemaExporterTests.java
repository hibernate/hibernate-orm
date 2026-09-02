/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.schema.spi.OracleSchemaExporters;
import org.hibernate.dialect.type.spi.UserDefinedTypeDdlSupport;
import org.hibernate.tool.schema.spi.StandardForeignKeyExporter;
import org.hibernate.tool.schema.spi.StandardIndexExporter;
import org.hibernate.tool.schema.spi.StandardSequenceExporter;
import org.hibernate.tool.schema.spi.StandardTableExporter;
import org.hibernate.tool.schema.spi.StandardUserDefinedTypeExporter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/// Tests construction and ownership of stock schema exporters.
///
/// @author Steve Ebersole
public class SchemaExporterTests {
	@Test
	void constructorsRejectANullOwner() {
		assertThatIllegalArgumentException().isThrownBy( () -> new StandardTableExporter( null ) );
		assertThatIllegalArgumentException().isThrownBy( () -> new StandardSequenceExporter( null ) );
		assertThatIllegalArgumentException().isThrownBy( () -> new StandardIndexExporter( null ) );
		assertThatIllegalArgumentException().isThrownBy( () -> new StandardForeignKeyExporter( null ) );
		assertThatIllegalArgumentException().isThrownBy( () -> new StandardUserDefinedTypeExporter( null ) );
		assertThatIllegalArgumentException().isThrownBy(
				() -> OracleSchemaExporters.userDefinedTypes( null, UserDefinedTypeDdlSupport.STANDARD ) );
		assertThatIllegalArgumentException().isThrownBy(
				() -> OracleSchemaExporters.userDefinedTypes( new OracleDialect(), null ) );
	}

	@Test
	void oracleFacadeCreatesExporterInstancesForItsOwner() {
		final OracleDialect oracle = new OracleDialect();
		assertThat( OracleSchemaExporters.userDefinedTypes( oracle, UserDefinedTypeDdlSupport.STANDARD ) )
				.isNotSameAs( OracleSchemaExporters.userDefinedTypes( oracle, UserDefinedTypeDdlSupport.STANDARD ) );
		assertThat( oracle.getUserDefinedTypeExporter() ).isNotNull();
	}
}
