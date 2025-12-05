/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema.scripts;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_IMPORT_FILES;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry( settings = @Setting( name = HBM2DDL_IMPORT_FILES, value = "/org/hibernate/orm/test/tool/schema/scripts/dogs.sql" ) )
@DomainModel(xmlMappings = {
		"/org/hibernate/orm/test/tool/schema/scripts/Human.hbm.xml",
		"/org/hibernate/orm/test/tool/schema/scripts/Dog.hbm.xml"
})
@SessionFactory
public class SingleLineImportFileTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testImportFile(SessionFactoryScope factoryScope) throws Exception {
		factoryScope.inTransaction( (session) -> {
			final List<?> humans = session.createQuery( "from " + Human.class.getName() ).list();
			Assertions.assertEquals( 3, humans.size(), "humans.sql not imported" );

			final List<?> dogs = session.createQuery( "from " + Dog.class.getName() ).list();
			Assertions.assertEquals( 3, dogs.size(), "dogs.sql not imported" );
		} );
	}
}
