/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.List;
import java.util.Map;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.support.domains.basic.BasicAuditedEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class ModifiedFlagSuffixTest extends AbstractModifiedFlagsEntityTest {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { BasicAuditedEntity.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.MODIFIED_FLAG_SUFFIX, "_CHANGED" );
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		id1 = inTransaction(
				entityManager -> {
					final BasicAuditedEntity entity = new BasicAuditedEntity( "x", 1L );
					entityManager.persist( entity );

					return entity.getId();
				}
		);
	}

	@DynamicTest
	public void testModFlagProperties() {
		assertThat(
				extractModifiedPropertyNames(
						BasicAuditedEntity.class.getName() + "_AUD",
						"_CHANGED"
				),
				containsInAnyOrder( "str1_CHANGED", "long1_CHANGED" )
		);
	}

	@DynamicTest
	public void testHasChanged() {
		final List str1List = queryForPropertyHasChangedWithDeleted( BasicAuditedEntity.class, id1, "str1" );
		assertThat( extractRevisions( str1List ), contains( 1 ) );

		final List long1List = queryForPropertyHasChangedWithDeleted( BasicAuditedEntity.class, id1, "long1" );
		assertThat( extractRevisions( long1List ), contains( 1 ) );

		final List queryResults = getAuditReader().createQuery()
				.forRevisionsOfEntity( BasicAuditedEntity.class, false, true )
				.add( AuditEntity.property( "str1" ).hasChanged() )
				.add( AuditEntity.property( "long1" ).hasChanged() )
				.getResultList();
		assertThat( extractRevisions( queryResults ), contains( 1 ) );
	}
}