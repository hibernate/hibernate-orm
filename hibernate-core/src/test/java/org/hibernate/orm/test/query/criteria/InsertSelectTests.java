/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.query.criteria;

import org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = InsertSelectTests.AnEntity.class)
@SessionFactory
public class InsertSelectTests {
	@Test
	public void simpleTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final SqmCriteriaNodeBuilder criteriaBuilder = (SqmCriteriaNodeBuilder) session.getCriteriaBuilder();
			final SqmInsertSelectStatement<AnEntity> insertSelect = criteriaBuilder.createCriteriaInsertSelect( AnEntity.class );
			final SqmSelectStatement<AnEntity> select = criteriaBuilder.createQuery( AnEntity.class );
			insertSelect.setSelectQueryPart( select.getQuerySpec() );
			select.from( AnEntity.class );
			session.createMutationQuery( insertSelect ).executeUpdate();
		} );
	}

	@SuppressWarnings("unused")
	@Entity( name = "AnEntity" )
	@Table( name = "AnEntity" )
	public static class AnEntity {
	    @Id
		@GeneratedValue
	    private Integer id;
	    @Basic
		private String name;

		private AnEntity() {
			// for use by Hibernate
		}

		public AnEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
