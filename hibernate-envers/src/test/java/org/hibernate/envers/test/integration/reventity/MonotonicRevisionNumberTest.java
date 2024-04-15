/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.reventity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.envers.enhanced.OrderedSequenceGenerator;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.entities.StrIntTestEntity;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7669")
@RequiresDialect(Oracle8iDialect.class)
public class MonotonicRevisionNumberTest extends BaseEnversFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {StrIntTestEntity.class}; // Otherwise revision entity is not generated.
	}

	@Test
	public void testOracleSequenceOrder() {
		EntityPersister persister = sessionFactory().getEntityPersister( SequenceIdRevisionEntity.class.getName() );
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
