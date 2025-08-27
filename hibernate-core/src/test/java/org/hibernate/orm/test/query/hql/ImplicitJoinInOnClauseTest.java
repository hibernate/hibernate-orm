/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author Christian Beikov
 */
@DomainModel( annotatedClasses = {
		ImplicitJoinInOnClauseTest.RootEntity.class,
		ImplicitJoinInOnClauseTest.FirstLevelReferencedEntity.class,
		ImplicitJoinInOnClauseTest.SecondLevelReferencedEntityA.class,
		ImplicitJoinInOnClauseTest.SecondLevelReferencedEntityB.class,
		ImplicitJoinInOnClauseTest.ThirdLevelReferencedEntity.class,
		ImplicitJoinInOnClauseTest.UnrelatedEntity.class
})
@SessionFactory
@JiraKey( "HHH-17033" )
public class ImplicitJoinInOnClauseTest {

	@Test
	public void testImplicitJoinInEntityJoinPredicate(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
						// this should get financial records which have a lastUpdateBy user set
						List<Object[]> result = session.createQuery(
								"select r.id, flr.id, ur1.id, ur2.id, ur3.id from RootEntity r " +
										"inner join r.firstLevelReference as flr " +
										"left join UnrelatedEntity ur1 on ur1.id = flr.secondLevelReferenceA.id " +
										"left join UnrelatedEntity ur2 on ur2.id = flr.secondLevelReferenceB.id " +
										"left join UnrelatedEntity ur3 on ur3.id = flr.secondLevelReferenceB.thirdLevelReference.id",
								Object[].class
						).list();
				}
		);
	}


	@Entity(name = "RootEntity")
	public static class RootEntity {
		@Id
		private Long id;
		@ManyToOne
		private FirstLevelReferencedEntity firstLevelReference;

	}

	@Entity(name = "FirstLevelReferencedEntity")
	public static class FirstLevelReferencedEntity {
		@Id
		private Long id;
		@ManyToOne
		private SecondLevelReferencedEntityA secondLevelReferenceA;
		@ManyToOne
		private SecondLevelReferencedEntityB secondLevelReferenceB;

	}
	@Entity(name = "SecondLevelReferencedEntityA")
	public static class SecondLevelReferencedEntityA {
		@Id
		private Long id;
		private String name;
	}

	@Entity(name = "SecondLevelReferencedEntityB")
	public static class SecondLevelReferencedEntityB {
		@Id
		private Long id;
		@ManyToOne
		private ThirdLevelReferencedEntity thirdLevelReference;
	}

	@Entity(name = "ThirdLevelReferencedEntity")
	public static class ThirdLevelReferencedEntity {
		@Id
		private Long id;
		private String name;

	}
	@Entity(name = "UnrelatedEntity")
	public static class UnrelatedEntity {
		@Id
		private Long id;
		private String name;
	}

}
