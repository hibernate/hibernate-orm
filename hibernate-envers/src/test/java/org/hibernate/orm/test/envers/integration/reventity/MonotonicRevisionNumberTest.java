/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.envers.enhanced.OrderedSequenceGenerator;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.orm.test.envers.BaseEnversFunctionalTestCase;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7669")
@RequiresDialect(OracleDialect.class)
public class MonotonicRevisionNumberTest extends BaseEnversFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {StrIntTestEntity.class}; // Otherwise revision entity is not generated.
	}

	@Test
	public void testOracleSequenceOrder() {
		EntityPersister persister = sessionFactory().getMappingMetamodel().getEntityDescriptor(SequenceIdRevisionEntity.class.getName());
		IdentifierGenerator generator = persister.getIdentifierGenerator();
		Assert.assertTrue( OrderedSequenceGenerator.class.isInstance( generator ) );

		Database database = metadata().getDatabase();
		SqlStringGenerationContext sqlStringGenerationContext =
				SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() );
		Optional<AuxiliaryDatabaseObject> sequenceOptional = database.getAuxiliaryDatabaseObjects().stream()
				.filter( o -> "REVISION_GENERATOR".equals( o.getExportIdentifier() ) )
				.findFirst();
		assertThat( sequenceOptional ).isPresent();
		String[] sqlCreateStrings = sequenceOptional.get().sqlCreateStrings( sqlStringGenerationContext );
		Assert.assertTrue(
				"Oracle sequence needs to be ordered in RAC environment.",
				sqlCreateStrings[0].toLowerCase().endsWith( " order" )
		);
	}
}
