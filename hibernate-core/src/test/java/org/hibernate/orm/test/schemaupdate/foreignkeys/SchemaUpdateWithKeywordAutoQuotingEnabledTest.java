/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys;

import java.util.EnumSet;
import java.util.Map;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.MappingSettings.KEYWORD_AUTO_QUOTING_ENABLED;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-11061")
@ServiceRegistry(settings = @Setting(name = KEYWORD_AUTO_QUOTING_ENABLED, value = "true"))
@DomainModel(annotatedClasses = SchemaUpdateWithKeywordAutoQuotingEnabledTest.Match.class)
public class SchemaUpdateWithKeywordAutoQuotingEnabledTest {

	@BeforeEach
	void createSchema(DomainModelScope modelScope) {
		final var metadata = modelScope.getDomainModel();
		metadata.orderColumns( false );
		metadata.validate();

		dropSchema( modelScope );

		new SchemaExport().setHaltOnError( true )
				.createOnly( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	@AfterEach
	void dropSchema(DomainModelScope modelScope) {
		final var metadata = modelScope.getDomainModel();
		metadata.orderColumns( false );
		metadata.validate();

		new SchemaExport()
				.drop( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	@Test
	public void testUpdate(DomainModelScope modelScope) {
		final var metadata = modelScope.getDomainModel();
		metadata.orderColumns( false );
		metadata.validate();

		new SchemaUpdate().setHaltOnError( true )
				.execute( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	@SuppressWarnings("unused")
	@Entity(name = "Match")
	@Table(name = "MATCH")
	public static class Match {
		@Id
		private Long id;

		@ElementCollection(fetch = FetchType.EAGER)
		@CollectionTable
		private Map<Integer, Integer> timeline;
	}
}
