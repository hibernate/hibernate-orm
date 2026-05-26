/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orphan.manytomany;

import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		xmlMappings = "org/hibernate/orm/test/orphan/manytomany/UserGroup.hbm.xml"
)
@SessionFactory
public class ManyToManyOrphanTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-8749")
	public void testManyToManyWithCascadeDeleteOrphan(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					User bob = new User( "bob", "jboss" );
					Group seam = new Group( "seam", "jboss" );
					seam.setGroupType( 1 );
					Group hb = new Group( "hibernate", "jboss" );
					hb.setGroupType( 2 );
					bob.getGroups().put( seam.getGroupType(), seam );
					bob.getGroups().put( hb.getGroupType(), hb );
					s.persist( bob );
					s.persist( seam );
					s.persist( hb );
				}
		);

		scope.inTransaction(
				s -> {
					User b = s.get( User.class, "bob" );
					assertEquals( 2, b.getGroups().size() );
					Group sG = s.get( Group.class, "seam" );
					assertEquals( (Integer) 1, sG.getGroupType() );
					Group hbG = s.get( Group.class, "hibernate" );
					assertEquals( (Integer) 2, hbG.getGroupType() );
				}
		);

		scope.inTransaction(
				s -> {
					User b = s.get( User.class, "bob" );
					assertEquals( 2, b.getGroups().size() );
					Group hG = s.get( Group.class, "hibernate" );
					b.getGroups().remove( hG.getGroupType() );
					assertEquals( 1, b.getGroups().size() );
				}
		);

		scope.inTransaction(
				s -> {
					User b = s.get( User.class, "bob" );
					assertEquals( 1, b.getGroups().size() );
				}
		);

		// Verify orphan group was deleted
		scope.inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Group> criteria = criteriaBuilder.createQuery( Group.class );
					criteria.from( Group.class );
					List<Group> groups = s.createQuery( criteria ).list();

//					List<Group> groups = s.createCriteria( Group.class ).list();
					assertEquals( 1, groups.size() );
					assertEquals( "seam", groups.get( 0 ).getName() );
				}
		);

	}
}
