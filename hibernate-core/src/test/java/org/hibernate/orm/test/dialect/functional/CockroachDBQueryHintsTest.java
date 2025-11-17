/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.dialect.CockroachDialect;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RequiresDialect(CockroachDialect.class)
@SessionFactory(useCollectingStatementInspector = true)
@DomainModel(annotatedClasses = {
		SimpleEntity.class, ChildEntity.class
})
@JiraKey("HHH-16867")
public class CockroachDBQueryHintsTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var se1 = new SimpleEntity( 1, "se1" );
			se1.addChild( new ChildEntity( "se1child1" ) );
			session.persist( se1 );
			var se2 = new SimpleEntity( 2, "se2" );
			session.persist( se2 );
			var se3 = new SimpleEntity( 3, "se3" );
			session.persist( se3 );
		} );
	}

	@Test
	public void testIndexHint(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			TypedQuery<Integer> query = session.createQuery( "select id from SimpleEntity where id < 3", Integer.class )
					.addQueryHint( "parents@{FORCE_INDEX=idx,DESC}" );
			var ignored = query.getResultList();
		} );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains(
				" parents@{FORCE_INDEX=idx,DESC} " );
	}

	@Test
	public void testJoinHint(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			TypedQuery<ChildEntity> query = session.createQuery(
							"select c from SimpleEntity s join s.children c where s.id < 3",
							ChildEntity.class
					)
					.addQueryHint( "haSh join" );
			var ignored = query.getResultList();
		} );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains(
				" hash join " );
	}

	@Test
	public void testOuterJoinHint(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			TypedQuery<ChildEntity> query = session.createQuery(
							"select c from SimpleEntity s left join s.children c where s.id < 3",
							ChildEntity.class
					)
					.addQueryHint( "haSh join" );
			var ignored = query.getResultList();
		} );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains(
				" hash join " );
	}
}

@Entity
@Table(name = "children")
class ChildEntity {
	@Id
	private Integer id;

	private String childName;

	@ManyToOne
	@JoinColumn(name = "parent_id", nullable = false)
	private SimpleEntity parent;

	public ChildEntity() {
	}

	public ChildEntity(String childName) {
		this.childName = childName;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public SimpleEntity getParent() {
		return parent;
	}
}

@Entity(name = "SimpleEntity")
@Table(name = "parents", indexes = { @Index(name = "idx", columnList = "id") })
class SimpleEntity {
	@Id
	private Integer id;

	private String name;

	@OneToMany(mappedBy = "parent")
	private Set<ChildEntity> children;

	public SimpleEntity() {
	}

	public SimpleEntity(Integer id, String name) {
		this.id = id;
		this.name = name;
		this.children = new HashSet<>();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	public Set<ChildEntity> getChildren() {
		return children;
	}

	public void setChildren(Set<ChildEntity> children) {
		this.children = children;
	}

	public void addChild(ChildEntity child) {
		this.children.add( child );
	}
}
