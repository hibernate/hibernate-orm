/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.integration.modifiedflags;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.integration.basic.BasicTestEntity1;
import org.hibernate.envers.test.tools.TestTools;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.envers.test.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.envers.test.tools.TestTools.makeList;

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
						getMetadata().getEntityBinding(
								"org.hibernate.envers.test.integration.basic.BasicTestEntity1_AUD"
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