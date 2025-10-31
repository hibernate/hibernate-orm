/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-10197")
@ServiceRegistry
@DomainModel(xmlMappings = "org/hibernate/orm/test/schemaupdate/UserGroup.hbm.xml")
public class QuotedTableNameWithForeignKeysSchemaUpdateTest {

	@BeforeEach
	public void setUp(DomainModelScope modelScope) {
		final MetadataImplementor domainModel = modelScope.getDomainModel();
		domainModel.orderColumns( false );
		domainModel.validate();
		new SchemaExport().create( EnumSet.of( TargetType.DATABASE ), domainModel );
	}

	@AfterEach
	public void tearDown(DomainModelScope modelScope) {
		new SchemaExport().drop( EnumSet.of( TargetType.STDOUT, TargetType.DATABASE ), modelScope.getDomainModel() );
	}

	@Test
	public void testUpdateExistingSchema(DomainModelScope modelScope) {
		new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE ), modelScope.getDomainModel() );
	}

	@Test
	public void testGeneratingUpdateScript(DomainModelScope modelScope) {
		new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE ), modelScope.getDomainModel() );
	}
}
