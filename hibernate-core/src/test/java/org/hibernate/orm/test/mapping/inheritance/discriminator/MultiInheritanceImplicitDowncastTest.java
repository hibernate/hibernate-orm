/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.discriminator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.Basic;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

/**
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				MultiInheritanceImplicitDowncastTest.IntIdEntity.class,
				MultiInheritanceImplicitDowncastTest.NameObject.class,
				MultiInheritanceImplicitDowncastTest.PolymorphicBase.class,
				MultiInheritanceImplicitDowncastTest.PolymorphicPropertyBase.class,
				MultiInheritanceImplicitDowncastTest.PolymorphicPropertyMapBase.class,
				MultiInheritanceImplicitDowncastTest.PolymorphicPropertySub1.class,
				MultiInheritanceImplicitDowncastTest.PolymorphicPropertySub2.class,
				MultiInheritanceImplicitDowncastTest.PolymorphicSub1.class,
				MultiInheritanceImplicitDowncastTest.PolymorphicSub2.class
		})
@SessionFactory(useCollectingStatementInspector = true)
public class MultiInheritanceImplicitDowncastTest {

	@Test
	public void testIllegalBaseJoin(SessionFactoryScope scope) {
		try {
			scope.inSession(
					s -> s.createQuery( "from PolymorphicPropertyBase p left join p.base b left join b.relation1", PolymorphicPropertyBase.class )
			);
		}
		catch (IllegalArgumentException ex) {
			Assertions.assertTrue( ex.getCause()
										.getCause()
										.getMessage()
										.contains( "Could not resolve attribute 'base' " ) );
		}
	}

	@Test
	public void testMultiJoinAddition1(SessionFactoryScope scope) {
		testMultiJoinAddition(
				scope,
				"base_sub_1",
				"select 1 from PolymorphicPropertyBase p left join treat(p as PolymorphicPropertySub1).base b left join b.relation1"
		);
	}

	@Test
	public void testMultiJoinAddition2(SessionFactoryScope scope) {
		testMultiJoinAddition(
				scope,
				"base_sub_2",
				"select 1 from PolymorphicPropertyBase p left join treat(p as PolymorphicPropertySub2).base b left join b.relation2"
		);
	}

	private void testMultiJoinAddition(SessionFactoryScope scope, String joinColumnBase, String hql) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				s -> {
					sqlStatementInterceptor.clear();
					s.createQuery( hql ).getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					final String generatedSql = sqlStatementInterceptor.getSqlQueries().get( 0 );

					int sub1JoinColumnIndex = generatedSql.indexOf( "." + joinColumnBase );
					Assertions.assertNotEquals(
							-1,
							sub1JoinColumnIndex,
							"Generated SQL doesn't contain a join for 'base' via '" + joinColumnBase + "':\n" + generatedSql
					);
				}
		);
	}

	@MappedSuperclass
	public abstract static class BaseEmbeddable<T extends PolymorphicBase> implements Serializable {
		private static final long serialVersionUID = 1L;

		private String someName;
		private T embeddedBase;

		public BaseEmbeddable() {
		}

		public String getSomeName() {
			return someName;
		}

		public void setSomeName(String someName) {
			this.someName = someName;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		public T getEmbeddedBase() {
			return embeddedBase;
		}

		public void setEmbeddedBase(T embeddedBase) {
			this.embeddedBase = embeddedBase;
		}
	}

	@Embeddable
	public abstract static class Embeddable1 extends BaseEmbeddable<PolymorphicSub1> {
		private static final long serialVersionUID = 1L;

		private String someName1;
		private PolymorphicSub1 embeddedRelation1;

		public Embeddable1() {
		}

		public String getSomeName1() {
			return someName1;
		}

		public void setSomeName1(String someName1) {
			this.someName1 = someName1;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		public PolymorphicSub1 getEmbeddedRelation1() {
			return embeddedRelation1;
		}

		public void setEmbeddedRelation1(PolymorphicSub1 embeddedRelation1) {
			this.embeddedRelation1 = embeddedRelation1;
		}
	}

	@Embeddable
	public abstract static class Embeddable2 extends BaseEmbeddable<PolymorphicSub2> {
		private static final long serialVersionUID = 1L;

		private String someName2;
		private PolymorphicSub2 embeddedRelation2;

		public Embeddable2() {
		}

		public String getSomeName2() {
			return someName2;
		}

		public void setSomeName2(String someName2) {
			this.someName2 = someName2;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		public PolymorphicSub2 getEmbeddedRelation2() {
			return embeddedRelation2;
		}

		public void setEmbeddedRelation2(PolymorphicSub2 embeddedRelation2) {
			this.embeddedRelation2 = embeddedRelation2;
		}
	}

	@Entity(name = "IntIdEntity")
	public static class IntIdEntity implements Serializable {
		private static final long serialVersionUID = 1L;

		private Integer id;
		private String name;

		public IntIdEntity() {
		}

		public IntIdEntity(String name) {
			this.name = name;
		}

		@Id
		@GeneratedValue
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Basic(optional = false)
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			IntIdEntity other = (IntIdEntity) obj;
			if ( id == null ) {
				if ( other.id != null ) {
					return false;
				}
			}
			else if ( !id.equals( other.id ) ) {
				return false;
			}
			return true;
		}
	}

	@Embeddable
	public static class NameObject implements Serializable {

		private String primaryName;
		private String secondaryName;
		private IntIdEntity intIdEntity;

		public NameObject() {
		}

		public NameObject(String primaryName, String secondaryName) {
			this.primaryName = primaryName;
			this.secondaryName = secondaryName;
		}

		public String getPrimaryName() {
			return primaryName;
		}

		public void setPrimaryName(String primaryName) {
			this.primaryName = primaryName;
		}

		public String getSecondaryName() {
			return secondaryName;
		}

		public void setSecondaryName(String secondaryName) {
			this.secondaryName = secondaryName;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "name_object_int_id_entity")
		public IntIdEntity getIntIdEntity() {
			return intIdEntity;
		}

		public void setIntIdEntity(IntIdEntity intIdEntity) {
			this.intIdEntity = intIdEntity;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof NameObject ) ) {
				return false;
			}

			NameObject that = (NameObject) o;

			if ( primaryName != null ? !primaryName.equals( that.primaryName ) : that.primaryName != null ) {
				return false;
			}
			return secondaryName != null ? secondaryName.equals( that.secondaryName ) : that.secondaryName == null;

		}

		@Override
		public int hashCode() {
			int result = primaryName != null ? primaryName.hashCode() : 0;
			result = 31 * result + ( secondaryName != null ? secondaryName.hashCode() : 0 );
			return result;
		}
	}

	@Entity(name = "PolymorphicBase")
	@Inheritance(strategy = InheritanceType.JOINED)
	public abstract static class PolymorphicBase implements Serializable {
		private static final long serialVersionUID = 1L;

		private Long id;
		private String name;
		private PolymorphicBase parent;
		private List<PolymorphicBase> list = new ArrayList<PolymorphicBase>();
		private Set<PolymorphicBase> children = new HashSet<PolymorphicBase>();
		private Map<String, PolymorphicBase> map = new HashMap<String, PolymorphicBase>();

		public PolymorphicBase() {
		}

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@ManyToOne(fetch = FetchType.LAZY, optional = true)
		public PolymorphicBase getParent() {
			return parent;
		}

		public void setParent(PolymorphicBase parent) {
			this.parent = parent;
		}

		@OneToMany
		@OrderColumn(name = "list_idx", nullable = false)
		@JoinTable(name = "polymorphic_list")
		public List<PolymorphicBase> getList() {
			return list;
		}

		public void setList(List<PolymorphicBase> list) {
			this.list = list;
		}

		@OneToMany(mappedBy = "parent")
		public Set<PolymorphicBase> getChildren() {
			return children;
		}

		public void setChildren(Set<PolymorphicBase> children) {
			this.children = children;
		}

		@OneToMany
		@JoinTable(name = "polymorphic_map")
		@MapKeyColumn(length = 20, nullable = false)
		public Map<String, PolymorphicBase> getMap() {
			return map;
		}

		public void setMap(Map<String, PolymorphicBase> map) {
			this.map = map;
		}
	}

	@Entity(name = "PolymorphicPropertyBase")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "PROP_TYPE")
	public abstract static class PolymorphicPropertyBase implements Serializable {

		private static final long serialVersionUID = 1L;

		private Long id;
		private String name;

		public PolymorphicPropertyBase() {
		}

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@MappedSuperclass
	public abstract static class PolymorphicPropertyMapBase<T extends PolymorphicBase> extends PolymorphicPropertyBase {

		private static final long serialVersionUID = 1L;

		private T base;
		private Set<T> bases;

		public PolymorphicPropertyMapBase() {
		}

		@ManyToOne(fetch = FetchType.LAZY)
		public T getBase() {
			return base;
		}

		public void setBase(T base) {
			this.base = base;
		}

		@OneToMany
		public Set<T> getBases() {
			return bases;
		}

		public void setBases(Set<T> bases) {
			this.bases = bases;
		}

	}

	@Entity(name = "PolymorphicPropertySub1")
	@AssociationOverrides({
			@AssociationOverride(name = "base", joinColumns = @JoinColumn(name = "base_sub_1"))
	})
	public static class PolymorphicPropertySub1 extends PolymorphicPropertyMapBase<PolymorphicSub1> {
		private static final long serialVersionUID = 1L;

		public PolymorphicPropertySub1() {
		}
	}

	@Entity(name = "PolymorphicPropertySub2")
	@AssociationOverrides({
			@AssociationOverride(name = "base", joinColumns = @JoinColumn(name = "base_sub_2"))
	})
	public static class PolymorphicPropertySub2 extends PolymorphicPropertyMapBase<PolymorphicSub2> {
		private static final long serialVersionUID = 1L;

		public PolymorphicPropertySub2() {
		}
	}

	@Entity(name = "PolymorphicSub1")
	public static class PolymorphicSub1 extends PolymorphicBase {
		private static final long serialVersionUID = 1L;

		private IntIdEntity relation1;
		private PolymorphicBase parent1;
		private NameObject embeddable1;
		private Integer sub1Value;

		public PolymorphicSub1() {
		}

		@ManyToOne(fetch = FetchType.LAZY)
		public IntIdEntity getRelation1() {
			return relation1;
		}

		public void setRelation1(IntIdEntity relation1) {
			this.relation1 = relation1;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		public PolymorphicBase getParent1() {
			return parent1;
		}

		public void setParent1(PolymorphicBase parent1) {
			this.parent1 = parent1;
		}

		@Embedded
		public NameObject getEmbeddable1() {
			return embeddable1;
		}

		public void setEmbeddable1(NameObject embeddable1) {
			this.embeddable1 = embeddable1;
		}

		public Integer getSub1Value() {
			return sub1Value;
		}

		public void setSub1Value(Integer sub1Value) {
			this.sub1Value = sub1Value;
		}
	}

	@Entity(name = "PolymorphicSub2")
	public static class PolymorphicSub2 extends PolymorphicBase {
		private static final long serialVersionUID = 1L;

		private IntIdEntity relation2;
		private PolymorphicBase parent2;
		private NameObject embeddable2;
		private Integer sub2Value;

		public PolymorphicSub2() {
		}

		@ManyToOne(fetch = FetchType.LAZY)
		public IntIdEntity getRelation2() {
			return relation2;
		}

		public void setRelation2(IntIdEntity relation2) {
			this.relation2 = relation2;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		public PolymorphicBase getParent2() {
			return parent2;
		}

		public void setParent2(PolymorphicBase parent1) {
			this.parent2 = parent1;
		}

		@Embedded
		public NameObject getEmbeddable2() {
			return embeddable2;
		}

		public void setEmbeddable2(NameObject embeddable1) {
			this.embeddable2 = embeddable1;
		}

		public Integer getSub2Value() {
			return sub2Value;
		}

		public void setSub2Value(Integer sub2Value) {
			this.sub2Value = sub2Value;
		}
	}
}
