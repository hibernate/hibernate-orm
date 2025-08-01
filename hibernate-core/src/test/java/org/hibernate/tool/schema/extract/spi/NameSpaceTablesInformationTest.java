/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.spi;

import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.mapping.Table;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.tool.schema.extract.internal.TableInformationImpl;
import org.hibernate.boot.model.relational.Namespace.Name;
import org.hibernate.boot.model.naming.Identifier;
import org.mockito.Mockito;


public class NameSpaceTablesInformationTest {

	@Test
	@JiraKey(value = "HHH-14270")
	public void testNameSpaceTablesInformation() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		JdbcEnvironment jdbcEnvironment = ssr.getService( JdbcEnvironment.class );
		IdentifierHelper identifierHelper = jdbcEnvironment.getIdentifierHelper();

		NameSpaceTablesInformation nameSpaceTablesInformation = new NameSpaceTablesInformation(identifierHelper);
		Name schemaName = new Name( new Identifier( "-", false ), new Identifier( "-", false ) );
		InformationExtractor informationExtractor = Mockito.mock( InformationExtractor.class );
		QualifiedTableName tableName = new QualifiedTableName( schemaName, new Identifier( "-", false ) );

		TableInformation tableInformation = new TableInformationImpl( informationExtractor, identifierHelper, tableName, false, null );
		nameSpaceTablesInformation.addTableInformation( tableInformation );
		final Table table = new Table( "orm", tableName.getTableName().getText() );
		nameSpaceTablesInformation.getTableInformation( table );
		boolean tableMatched = tableInformation.getName().getTableName().getText().equals( tableName.getTableName().getText() );
		Assert.assertTrue("Table matched: ", tableMatched);
	}
}
