/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.revisionentity;

import java.sql.Types;
import java.util.List;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.inheritance.joined.ChildEntity;
import org.hibernate.envers.test.support.domains.inheritance.joined.ParentEntity;
import org.hibernate.envers.test.support.domains.revisionentity.LongRevNumberRevEntity;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * A join-inheritance test using a custom revision entity where the revision number is a long, mapped in the database
 * as an int.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Disabled("NYI - Joined inheritance support")
public class LongRevEntityInheritanceChildAuditingTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ChildEntity.class, ParentEntity.class, LongRevNumberRevEntity.class };
	}

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void testChildRevColumnType() {
		final List<Column> idColumns = getAuditEntityDescriptor( ChildEntity.class ).getIdentifierDescriptor().getColumns();

		assertThat( idColumns, CollectionMatchers.hasSize( 2 ) );

		// The LongRevNumberRevEntity uses a long java type; however explicitly sets column definition an integer
		// In the audited entity class, the 'REV' column should match the same explicit column definition type, integer.
		assertThat( idColumns.get( 1 ).getSqlTypeDescriptor().getJdbcTypeCode(), equalTo( Types.INTEGER ) );
	}
}