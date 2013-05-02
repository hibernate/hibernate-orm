package org.hibernate.envers.test.integration.reventity;

import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.envers.enhanced.OrderedSequenceGenerator;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.entities.StrIntTestEntity;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.persister.entity.EntityPersister;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;

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
		OrderedSequenceGenerator seqGenerator = (OrderedSequenceGenerator) generator;
		Assert.assertTrue(
				"Oracle sequence needs to be ordered in RAC environment.",
				seqGenerator.sqlCreateStrings( getDialect() )[0].endsWith( " order" )
		);
	}
}
