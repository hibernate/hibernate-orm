/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.refcolnames.superclass;

import jakarta.persistence.*;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

@SessionFactory
@DomainModel(annotatedClasses = {
		ReferencedSuperclassColumnTest.Branch.class,
		ReferencedSuperclassColumnTest.LocalBranch.class,
		ReferencedSuperclassColumnTest.DivisionHead.class,
		ReferencedSuperclassColumnTest.RegionHead.class,
		ReferencedSuperclassColumnTest.CircleHead.class
})
public class ReferencedSuperclassColumnTest {

	@Test
	void test(SessionFactoryScope scope) {
		LocalBranch localBranch = new LocalBranch();
		localBranch.branchId = "local";
		DivisionHead divisionHead = new DivisionHead();
		divisionHead.branchId = "head";
		localBranch.divisionHead = divisionHead;
		divisionHead.localBranches.add(localBranch);
		scope.inTransaction( s -> s.persist(divisionHead));
		scope.inTransaction( s -> {
			DivisionHead dh = s.find(DivisionHead.class, divisionHead.id);
			Assertions.assertEquals(1, dh.localBranches.size());
		});
		scope.inTransaction( s -> {
			LocalBranch lb = s.find(LocalBranch.class, localBranch.id);
			Assertions.assertEquals(divisionHead.id, lb.divisionHead.id);
		});
		scope.inTransaction( s -> {
			LocalBranch lb = s.createQuery("from LocalBranch left join fetch divisionHead", LocalBranch.class)
					.getSingleResult();
			Assertions.assertEquals(divisionHead.id, lb.divisionHead.id);
		});
		scope.inTransaction( s -> {
			DivisionHead dh = s.createQuery("from DivisionHead left join fetch localBranches", DivisionHead.class)
					.getSingleResult();
			Assertions.assertEquals(1, dh.localBranches.size());
		});
	}

	@Entity(name="Branch")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "branch_type", discriminatorType = DiscriminatorType.STRING)
	public static class Branch {
		@Id
		@GeneratedValue
		Long id;

		@Column(unique = true, name = "branch_id")
		String branchId;
	}

	@Entity(name="LocalBranch")
	@DiscriminatorValue("LOCAL")
	public static class LocalBranch extends Branch {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "division_head_id", referencedColumnName = "branch_id")
		DivisionHead divisionHead;

		// other fields and mappings

		// getters and setters
	}

	@Entity(name="DivisionHead")
	@DiscriminatorValue("DIVISION")
	public static class DivisionHead extends Branch {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "region_head_id", referencedColumnName = "branch_id")
		RegionHead regionHead;

		@OneToMany(mappedBy = "divisionHead", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		Set<LocalBranch> localBranches = new HashSet<>();

		// other fields and mappings

		// getters and setters
	}

	@Entity(name="RegionHead")
	@DiscriminatorValue("REGION")
	public static class RegionHead extends Branch {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "circle_head_id", referencedColumnName = "branch_id")
		CircleHead circleHead;

		@OneToMany(mappedBy = "regionHead", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		Set<DivisionHead> divisions = new HashSet<>();

		// other fields and mappings

		// getters and setters
	}

	@Entity(name="CircleHead")
	@DiscriminatorValue("CIRCLE")
	public static class CircleHead extends Branch {
		@OneToMany(mappedBy = "circleHead", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		Set<RegionHead> regions = new HashSet<>();

		// other fields and mappings

		// getters and setters
	}
}
