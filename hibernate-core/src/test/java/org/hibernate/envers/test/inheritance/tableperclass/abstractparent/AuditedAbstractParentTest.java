/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.inheritance.tableperclass.abstractparent;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.inheritance.tableperclass.abstractparent.AbstractEntity;
import org.hibernate.envers.test.support.domains.inheritance.tableperclass.abstractparent.EffectiveEntity1;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-5910")
@Disabled("NYI - Inheritance")
public class AuditedAbstractParentTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { AbstractEntity.class, EffectiveEntity1.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransaction(
				entityManager -> {
					EffectiveEntity1 entity = new EffectiveEntity1( 1L, "commonField", "specificField1" );
					entityManager.persist( entity );
				}
		);
	}

	@DynamicTest
	@Disabled("Need to implement this test check")
	public void testAbstractTableExistence() {
//		for ( Table table : metadata().collectTableMappings() ) {
//			if ( "AbstractEntity_AUD".equals( table.getName() ) ) {
//				Assert.assertFalse( table.isPhysicalTable() );
//				return;
//			}
//		}
//		Assert.fail();
	}
}
