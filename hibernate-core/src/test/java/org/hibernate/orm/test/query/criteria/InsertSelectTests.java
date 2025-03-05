/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder;
import org.hibernate.query.sqm.tree.from.SqmRoot;
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
import jakarta.persistence.Tuple;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = InsertSelectTests.AnEntity.class)
@SessionFactory
public class InsertSelectTests {
	@Test
	public void simpleTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new AnEntity( "test" ) );
			final SqmCriteriaNodeBuilder criteriaBuilder = (SqmCriteriaNodeBuilder) session.getCriteriaBuilder();
			final SqmInsertSelectStatement<AnEntity> insertSelect = criteriaBuilder.createCriteriaInsertSelect( AnEntity.class );
			final SqmSelectStatement<Tuple> select = criteriaBuilder.createQuery( Tuple.class );
			insertSelect.addInsertTargetStateField( insertSelect.getTarget().get( "name" ) );
			final SqmRoot<AnEntity> root = select.from( AnEntity.class );
			select.multiselect( root.get( "name" ) );
			insertSelect.setSelectQueryPart( select.getQuerySpec() );
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

		public AnEntity(String name) {
			this.name = name;
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
