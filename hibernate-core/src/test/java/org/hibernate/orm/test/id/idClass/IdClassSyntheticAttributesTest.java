/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.idClass;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = MyEntity.class
)
@SessionFactory
public class IdClassSyntheticAttributesTest {

	@Jira("https://hibernate.atlassian.net/browse/HHH-18841")
	@Test
	public void test(DomainModelScope scope) {
		final PersistentClass entityBinding = scope.getDomainModel().getEntityBinding(MyEntity.class.getName());
		assertThat(entityBinding.getProperties()).hasSize(2)
				.anySatisfy(p -> {
					assertThat(p.isSynthetic()).isTrue();
					assertThat(p.getName()).isEqualTo("_identifierMapper");
				})
				.anySatisfy(p -> {
					assertThat(p.isSynthetic()).isFalse();
					assertThat(p.getName()).isEqualTo("notes");
				});
	}
}
