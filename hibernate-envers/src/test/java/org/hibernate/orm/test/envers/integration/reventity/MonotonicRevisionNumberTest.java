/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.envers.enhanced.OrderedSequenceGenerator;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7669")
@RequiresDialect(OracleDialect.class)
@EnversTest
@DomainModel(annotatedClasses = {StrIntTestEntity.class})
@SessionFactory
public class MonotonicRevisionNumberTest {
	@Test
	public void testOracleSequenceOrder(SessionFactoryScope scope, DomainModelScope domainModelScope) {
		EntityPersister persister = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( SequenceIdRevisionEntity.class.getName() );
		IdentifierGenerator generator = persister.getIdentifierGenerator();
		assertTrue( OrderedSequenceGenerator.class.isInstance( generator ) );

		Database database = domainModelScope.getDomainModel().getDatabase();
		SqlStringGenerationContext sqlStringGenerationContext =
				SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() );
		Optional<AuxiliaryDatabaseObject> sequenceOptional = database.getAuxiliaryDatabaseObjects().stream()
				.filter( o -> "REVISION_GENERATOR".equals( o.getExportIdentifier() ) )
				.findFirst();
		assertThat( sequenceOptional ).isPresent();
		String[] sqlCreateStrings = sequenceOptional.get().sqlCreateStrings( sqlStringGenerationContext );
		assertTrue(
				sqlCreateStrings[0].toLowerCase().endsWith( " order" ),
				"Oracle sequence needs to be ordered in RAC environment."
		);
	}
}
