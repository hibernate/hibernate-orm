/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;
import java.util.Map;
import jakarta.persistence.EntityManager;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.integration.basic.BasicTestEntity1;
import org.hibernate.orm.test.envers.tools.TestTools;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.orm.test.envers.tools.TestTools.makeList;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class ModifiedFlagSuffix extends AbstractModifiedFlagsEntityTest {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {BasicTestEntity1.class};
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( EnversSettings.MODIFIED_FLAG_SUFFIX, "_CHANGED" );
	}

	private Integer addNewEntity(String str, long lng) {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		BasicTestEntity1 bte1 = new BasicTestEntity1( str, lng );
		em.persist( bte1 );
		em.getTransaction().commit();

		return bte1.getId();
	}

	@Test
	@Priority(10)
	public void initData() {
		id1 = addNewEntity( "x", 1 ); // rev 1
	}

	@Test
	public void testModFlagProperties() {
		assertEquals(
				TestTools.makeSet( "str1_CHANGED", "long1_CHANGED" ),
				TestTools.extractModProperties(
						metadata().getEntityBinding(
								"org.hibernate.orm.test.envers.integration.basic.BasicTestEntity1_AUD"
						),
						"_CHANGED"
				)
		);
	}

	@Test
	public void testHasChanged() throws Exception {
		List list = queryForPropertyHasChangedWithDeleted(
				BasicTestEntity1.class,
				id1, "str1"
		);
		assertEquals( 1, list.size() );
		assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );

		list = queryForPropertyHasChangedWithDeleted(
				BasicTestEntity1.class,
				id1, "long1"
		);
		assertEquals( 1, list.size() );
		assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );

		list = getAuditReader().createQuery().forRevisionsOfEntity( BasicTestEntity1.class, false, true )
				.add( AuditEntity.property( "str1" ).hasChanged() )
				.add( AuditEntity.property( "long1" ).hasChanged() )
				.getResultList();
		assertEquals( 1, list.size() );
		assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );
	}
}
