/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.revisionentity;

import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.envers.enhanced.OrderedSequenceGenerator;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrIntTestEntity;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;
import org.hibernate.testing.orm.junit.RequiresDialect;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7669")
@RequiresDialect(Oracle8iDialect.class)
public class MonotonicRevisionNumberTest extends EnversSessionFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StrIntTestEntity.class }; // Otherwise revision entity is not generated.
	}

	@DynamicTest
	public void testOracleSequenceOrder() {
		final EntityTypeDescriptor entityDescriptor = getMetamodel().getEntityDescriptor( SequenceIdRevisionEntity.class );

		IdentifierGenerator generator = entityDescriptor.getHierarchy().getIdentifierDescriptor().getIdentifierValueGenerator();
		assertThat( generator, instanceOf( OrderedSequenceGenerator.class ) );

		OrderedSequenceGenerator seqGenerator = (OrderedSequenceGenerator) generator;
		assertThat(
				"Oracle sequence needs to be stored in RAC environment.",
				seqGenerator.sqlCreateStrings( getDialect() )[0].toLowerCase().endsWith( " order" ),
				is( true )
		);
	}
}
