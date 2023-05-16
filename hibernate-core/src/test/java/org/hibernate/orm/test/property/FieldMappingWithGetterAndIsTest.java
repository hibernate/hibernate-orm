/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.property;

import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = FieldMappingWithGetterAndIsTest.Tester.class)
@SessionFactory
public class FieldMappingWithGetterAndIsTest {
	@Test
	public void testResolution(DomainModelScope modelScope, SessionFactoryScope factoryScope) {
		final PersistentClass entityBinding = modelScope.getEntityBinding( Tester.class );
		factoryScope.getCollectingStatementInspector();
	}

	@Entity(name="Tester")
	@Table(name="Tester")
	public static class Tester {
		@Id
		private Integer id;
		@Basic
		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public boolean isName() {
			return name != null;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
