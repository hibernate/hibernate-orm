/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( annotatedClasses = {
		TreatJoinOptimizationTest.JoinedBase.class,
		TreatJoinOptimizationTest.JoinedSub1.class
} )
@SessionFactory
@JiraKey( "HHH-17385" )
public class TreatJoinOptimizationTest {

	@Test
	public void testSelectSuperclassAttributeOfJoinedSubclassCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createSelectionQuery(
				"SELECT s1.value FROM JoinedBase b JOIN b.children s1",
				Integer.class
			).getResultList();
		} );
	}

	@SuppressWarnings("unused")
	@Entity( name = "JoinedBase" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static abstract class JoinedBase {
		@Id
		private Long id;
		@Column(name = "val")
		private Integer value;
	}

	@SuppressWarnings({"FieldMayBeFinal", "unused"})
	@Entity( name = "JoinedSub1" )
	@Table( name = "joined_sub_1" )
	public static class JoinedSub1 extends JoinedBase {
		@OneToMany
		private Set<JoinedSub1> children = new HashSet<>();
	}
}
