/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.formula;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Formula;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-12227" )
@RequiresDialect( value = H2Dialect.class, comment = "Not dialect specific" )
@SuppressWarnings("JUnitMalformedDeclaration")
public class SchemaSubstitutionTests {
	@Test
	@ServiceRegistry( settings = @Setting( name = MappingSettings.DEFAULT_SCHEMA, value = "my_schema" ) )
	@DomainModel( annotatedClasses = Thing.class )
	void testWithSchema(DomainModelScope modelScope) {
		try (SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) modelScope.getDomainModel().buildSessionFactory()) {
			final EntityPersister persister = sessionFactory.getMappingMetamodel().getEntityDescriptor( Thing.class );
			final AttributeMapping attributeMapping = persister.findAttributeMapping( "externalName" );
			verifyFormula( attributeMapping, true );
		}
	}

	@Test
	@ServiceRegistry
	@DomainModel( annotatedClasses = Thing.class )
	void testWithoutSchema(DomainModelScope modelScope) {
		try (SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) modelScope.getDomainModel().buildSessionFactory()) {
			final EntityPersister persister = sessionFactory.getMappingMetamodel().getEntityDescriptor( Thing.class );
			final AttributeMapping attributeMapping = persister.findAttributeMapping( "externalName" );
			verifyFormula( attributeMapping, false );
		}
	}

	private void verifyFormula(AttributeMapping attributeMapping, boolean expectSchema) {
		final SelectableMapping selectable = attributeMapping.getSelectable( 0 );
		assertThat( selectable.isFormula() ).isTrue();
		assertThat( selectable.getSelectionExpression() ).doesNotContain( "{h-schema}" );
		assertThat( selectable.getSelectionExpression().contains( "my_schema" ) ).isEqualTo( expectSchema );
	}

	@Entity(name="Thing")
	@Table(name="things")
	public static class Thing {
		@Id
		private Integer id;
		private String name;
		@Formula( "select name from {h-schema}externals" )
		private String externalName;
	}
}
