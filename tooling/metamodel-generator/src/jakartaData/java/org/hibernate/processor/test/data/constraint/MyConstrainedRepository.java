/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.constraint;

import java.util.List;

import jakarta.data.constraint.AtLeast;
import jakarta.data.constraint.AtMost;
import jakarta.data.constraint.Constraint;
import jakarta.data.constraint.GreaterThan;
import jakarta.data.constraint.In;
import jakarta.data.constraint.LessThan;
import jakarta.data.constraint.Like;
import jakarta.data.constraint.NotEqualTo;
import jakarta.data.constraint.NotIn;
import jakarta.data.constraint.NotLike;
import jakarta.data.repository.By;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Is;
import jakarta.data.repository.Repository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Repository
public interface MyConstrainedRepository extends CrudRepository<MyEntity, Long> {

	@Valid
	@NotNull
	@Find
	MyEntity findByName(@NotNull @Size(min = 5) String name);

	@Find
	List<MyEntity> findByNameLike(@By("name") @Is(Like.class) String pattern);

	@Find
	List<MyEntity> findByNameNotLike(@By("name") @Is(NotLike.class) String pattern);

	@Find
	List<MyEntity> findByNameNot(@By("name") @Is(NotEqualTo.class) String name);

	@Find
	List<MyEntity> findByNames(@By("name") @Is(In.class) List<String> names);

	@Find
	List<MyEntity> findByNamesNot(@By("name") @Is(NotIn.class) String[] names);

	@Find
	List<MyEntity> findByNameConstraint(@By("name") Constraint<String> name);

	@Find
	List<MyEntity> findByNameWildcardConstraint(@By("name") Constraint<? super String> name);

	@Find
	List<MyEntity> findByNameLikeConstraint(@By("name") Like name);

	@Find
	List<MyEntity> findByAgeGreaterThan(@By("age") @Is(GreaterThan.class) Integer age);

	@Find
	List<MyEntity> findByAgeLessThan(@By("age") @Is(LessThan.class) Integer age);

	@Find
	List<MyEntity> findByAgeAtLeastAndAtMost(
			@By("age") @Is(AtLeast.class) Integer minAge,
			@By("age") @Is(AtMost.class) Integer maxAge);

	@Delete
	void deleteByNameNot(@By("name") @Is(NotEqualTo.class) String name);
}
