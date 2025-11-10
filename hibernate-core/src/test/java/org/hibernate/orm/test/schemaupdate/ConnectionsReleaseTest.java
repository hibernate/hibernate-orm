/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.jdbc.SharedDriverManagerConnectionProvider;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.EnumSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-10443")
@RequiresDialect( H2Dialect.class )
@ServiceRegistryFunctionalTesting
@DomainModel(annotatedClasses = ConnectionsReleaseTest.Thing.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConnectionsReleaseTest implements ServiceRegistryProducer {

	@BeforeAll
	static void beforeAll(DomainModelScope modelScope) {
		modelScope.getDomainModel().orderColumns( false );
		modelScope.getDomainModel().validate();
	}

	@Test
	@Order(1)
	public void testSchemaUpdateReleasesAllConnections(DomainModelScope modelScope) {
		new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE ), modelScope.getDomainModel() );
		assertThat( SharedDriverManagerConnectionProvider.getInstance().getOpenConnections(), is( 0 ) );
	}

	@Test
	@Order(2)
	public void testSchemaValidatorReleasesAllConnections(DomainModelScope modelScope) {
		new SchemaValidator().validate( modelScope.getDomainModel() );
		assertThat( SharedDriverManagerConnectionProvider.getInstance().getOpenConnections(), is( 0 ) );
	}

	@Entity(name = "Thing")
	@Table(name = "Thing")
	public static class Thing {
		@Id
		public Integer id;
	}

}
