/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.test.integration.customtype;

import java.util.Arrays;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.customtype.ParametrizedCustomTypeEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ParametrizedCustom extends BaseEnversJPAFunctionalTestCase {
	private Integer pcte_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ParametrizedCustomTypeEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		ParametrizedCustomTypeEntity pcte = new ParametrizedCustomTypeEntity();

		// Revision 1 (persisting 1 entity)
		em.getTransaction().begin();

		pcte.setStr( "U" );

		em.persist( pcte );

		em.getTransaction().commit();

		// Revision 2 (changing the value)
		em.getTransaction().begin();

		pcte = em.find( ParametrizedCustomTypeEntity.class, pcte.getId() );

		pcte.setStr( "V" );

		em.getTransaction().commit();

		//

		pcte_id = pcte.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals(
				getAuditReader().getRevisions(
						ParametrizedCustomTypeEntity.class,
						pcte_id
				)
		);
	}

	@Test
	public void testHistoryOfCcte() {
		ParametrizedCustomTypeEntity rev1 = getAuditReader().find( ParametrizedCustomTypeEntity.class, pcte_id, 1 );
		ParametrizedCustomTypeEntity rev2 = getAuditReader().find( ParametrizedCustomTypeEntity.class, pcte_id, 2 );

		assert "xUy".equals( rev1.getStr() );
		assert "xVy".equals( rev2.getStr() );
	}
}
