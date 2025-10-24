/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.JoinType;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.order.NullPrecedence;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@SuppressWarnings("rawtypes")
@JiraKey(value = "HHH-11895")
@Jpa(annotatedClasses = {
		ComponentQueryTest.Asset.class,
		ComponentQueryTest.Symbol.class,
		ComponentQueryTest.SymbolType.class
})
@EnversTest
public class ComponentQueryTest {

	@Embeddable
	public static class Symbol {

		@ManyToOne
		private SymbolType type;
		private String identifier;

		public Symbol() {

		}

		public Symbol(final SymbolType type, final String identifier) {
			this.type = type;
			this.identifier = identifier;
		}

		public SymbolType getType() {
			return type;
		}

		public void setType(SymbolType type) {
			this.type = type;
		}

		public String getIdentifier() {
			return identifier;
		}

		public void setIdentifier(String identifier) {
			this.identifier = identifier;
		}

	}

	@Entity(name = "SymbolType")
	@Audited
	public static class SymbolType {

		@Id
		@GeneratedValue
		private Integer id;

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

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Asset")
	@Audited
	public static class Asset {

		@Id
		@GeneratedValue
		private Integer id;

		@Embedded
		private Symbol singleSymbol;

		@ElementCollection
		private Set<Symbol> multiSymbols = new HashSet<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Symbol getSingleSymbol() {
			return singleSymbol;
		}

		public void setSingleSymbol(Symbol singleSymbol) {
			this.singleSymbol = singleSymbol;
		}

		public Set<Symbol> getMultiSymbols() {
			return multiSymbols;
		}

		public void setMultiSymbols(Set<Symbol> multiSymbols) {
			this.multiSymbols = multiSymbols;
		}

	}

	private SymbolType type1;
	private SymbolType type2;

	private Asset asset1;
	private Asset asset2;
	private Asset asset3;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			type1 = new SymbolType();
			type1.setName( "T1" );
			em.persist( type1 );

			type2 = new SymbolType();
			type2.setName( "T2" );
			em.persist( type2 );

			asset1 = new Asset();
			em.persist( asset1 );

			asset2 = new Asset();
			asset2.setSingleSymbol( new Symbol( type1, "X1" ) );
			asset2.getMultiSymbols().add( new Symbol( type1, "X" ) );
			em.persist( asset2 );

			asset3 = new Asset();
			asset3.setSingleSymbol( new Symbol( type2, "X2" ) );
			asset3.getMultiSymbols().add( new Symbol( type1, "Y" ) );
			asset3.getMultiSymbols().add( new Symbol( type2, "X" ) );
			em.persist( asset3 );
		} );
	}

	@Test
	public void testSingleSymbolUsingIdentifier(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List actual = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Asset.class, 1 )
					.traverseRelation( "singleSymbol", JoinType.INNER, "s" )
					.add( AuditEntity.property( "s", "identifier" ).eq( "X1" ) )
					.up()
					.addProjection( AuditEntity.id() )
					.getResultList();
			assertEquals( Collections.singletonList( asset2.getId() ), actual, "Expected only asset2 to be returned" );
		} );
	}

	@Test
	public void testMultiSymbolUsingIdentifier(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List actual = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Asset.class, 1 )
					.traverseRelation( "multiSymbols", JoinType.INNER, "s" )
					.add( AuditEntity.property( "s", "identifier" ).like( "X%" ) )
					.up()
					.addProjection( AuditEntity.id() )
					.addOrder( AuditEntity.id().asc() )
					.getResultList();
			List<Integer> expected = new ArrayList<>();
			Collections.addAll( expected, asset2.getId(), asset3.getId() );
			assertEquals( expected, actual, "Expected only the ids of the assets with symbol T1" );
		} );
	}

	@Test
	public void testSingleSymbolUsingType(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List actual = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Asset.class, 1 )
					.traverseRelation( "singleSymbol", JoinType.INNER, "s" )
					.add( AuditEntity.property( "s", "type" ).eq( type1 ) )
					.up()
					.addProjection( AuditEntity.id() )
					.getResultList();
			assertEquals( Collections.singletonList( asset2.getId() ), actual, "Expected only asset2 to be returned" );
		} );
	}

	@Test
	public void testMultiSymbolUsingType(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List actual = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Asset.class, 1 )
					.traverseRelation( "multiSymbols", JoinType.INNER, "s" )
					.add( AuditEntity.property( "s", "type" ).eq( type2 ) )
					.up()
					.addProjection( AuditEntity.id() )
					.getResultList();
			assertEquals( Collections.singletonList( asset3.getId() ), actual, " Expected only asset3 to be returned" );
		} );
	}

	@Test
	public void testJoinOnSingleComponentAssociation(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List actual = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Asset.class, 1 )
					.traverseRelation( "singleSymbol", JoinType.INNER, "s" )
					.traverseRelation( "type", JoinType.INNER, "t" )
					.add( AuditEntity.property( "t", "name" ).eq( "T1" ) )
					.up()
					.up()
					.addProjection( AuditEntity.id() )
					.getResultList();
			assertEquals( Collections.singletonList( asset2.getId() ), actual, "Expected only asset2 to be returned" );
		} );
	}

	@Test
	public void testJoinOnMultiComponentAssociation(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List actual = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Asset.class, 1 )
					.traverseRelation( "multiSymbols", JoinType.INNER, "s" )
					.traverseRelation( "type", JoinType.INNER, "t" )
					.add( AuditEntity.property( "t", "name" ).eq( "T2" ) )
					.up()
					.up()
					.addProjection( AuditEntity.id() )
					.getResultList();
			assertEquals( Collections.singletonList( asset3.getId() ), actual, "Expected only asset3 to be returned" );
		} );
	}

	@Test
	public void testOrderingOnSingleComponent(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List actual = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Asset.class, 1 )
					.addProjection( AuditEntity.id() )
					.traverseRelation( "singleSymbol", JoinType.LEFT, "s" )
					.addOrder( AuditEntity.property( "s", "identifier" ).asc().nulls( NullPrecedence.FIRST ) )
					.getResultList();
			List<Integer> expected = new ArrayList<>();
			Collections.addAll( expected, asset1.getId(), asset2.getId(), asset3.getId() );
			assertEquals( expected, actual, "Expected all assets in correct order" );
		} );
	}

	@Test
	public void testOrderingOnMultiComponent(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List actual = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Asset.class, 1 )
					.addProjection( AuditEntity.id() )
					.traverseRelation( "multiSymbols", JoinType.LEFT, "s" )
					.traverseRelation( "type", JoinType.LEFT, "t" )
					.addOrder( AuditEntity.property( "t", "name" ).asc().nulls( NullPrecedence.FIRST ) )
					.addOrder( AuditEntity.property( "s", "identifier" ).asc().nulls( NullPrecedence.FIRST ) )
					.getResultList();
			List<Integer> expected = new ArrayList<>();
			Collections.addAll( expected, asset1.getId(), asset2.getId(), asset3.getId(), asset3.getId() );
			assertEquals( expected, actual, "Expected all assets in correct order" );
		} );
	}

	@Test
	public void testProjectionOnSingleComponentProperty(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List actual = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Asset.class, 1 )
					.add( AuditEntity.id().eq( asset2.getId() ) )
					.traverseRelation( "singleSymbol", JoinType.INNER, "s" )
					.addProjection( AuditEntity.property( "s", "identifier" ) )
					.getResultList();
			assertEquals( Collections.singletonList( "X1" ), actual, "Expected the symbol identifier of asset2 to be returned" );
		} );
	}

	@Test
	public void testProjectionOnMultiComponentProperty(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List actual = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Asset.class, 1 )
					.add( AuditEntity.id().eq( asset2.getId() ) )
					.traverseRelation( "multiSymbols", JoinType.INNER, "s" )
					.addProjection( AuditEntity.property( "s", "identifier" ) )
					.getResultList();
			assertEquals( Collections.singletonList( "X" ), actual, "Expected the symbol identifier of asset2 to be returned" );
		} );
	}

	@Test
	public void testFunctionOnSingleComponentProperty(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List actual = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Asset.class, 1 )
					.add( AuditEntity.id().eq( asset2.getId() ) )
					.traverseRelation( "singleSymbol", JoinType.INNER, "s" )
					.addProjection( AuditEntity.function( "CONCAT", AuditEntity.property( "s", "identifier" ), "Z" ) )
					.getResultList();
			assertEquals( Collections.singletonList( "X1Z" ), actual, "Expecte the symbol identfier of asset2 concatenated with 'Z'" );
		} );
	}

	@Test
	public void testFunctionOnMultiComponentProperty(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List actual = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Asset.class, 1 )
					.add( AuditEntity.id().eq( asset2.getId() ) )
					.traverseRelation( "multiSymbols", JoinType.INNER, "s" )
					.addProjection( AuditEntity.function( "CONCAT", AuditEntity.property( "s", "identifier" ), "Z" ) )
					.getResultList();
			assertEquals( Collections.singletonList( "XZ" ), actual, "Expecte the symbol identfier of asset2 concatenated with 'Z'" );
		} );
	}

	@Test
	@JiraKey(value = "HHH-11896")
	public void testOnClauseOnSingleSymbol(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List actual = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Asset.class, 1 )
					.addProjection( AuditEntity.id() )
					.traverseRelation( "singleSymbol", JoinType.LEFT, "s", AuditEntity.property( "s", "type" ).eq( type1 ) )
					.addOrder( AuditEntity.property( "s", "identifier" ).asc().nulls( NullPrecedence.FIRST ) )
					.up()
					.addOrder( AuditEntity.id().asc() )
					.getResultList();
			final List<Integer> expected = new ArrayList<>();
			Collections.addAll( expected, asset1.getId(), asset3.getId(), asset2.getId() );
			assertEquals( expected, actual, "Expected the correct ordering. Assets which do not have a symbol of type1 should come first (null first)" );
		} );
	}

	@Test
	@JiraKey(value = "HHH-11896")
	public void testOnClauseOnMultiSymbol(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List actual = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Asset.class, 1 )
					.addProjection( AuditEntity.id() )
					.traverseRelation( "multiSymbols", JoinType.LEFT, "s", AuditEntity.property( "s", "type" ).eq( type1 ) )
					.addOrder( AuditEntity.property( "s", "identifier" ).asc().nulls( NullPrecedence.FIRST ) )
					.up()
					.addOrder( AuditEntity.id().asc() )
					.getResultList();
			final List<Integer> expected = new ArrayList<>();
			Collections.addAll( expected, asset1.getId(), asset2.getId(), asset3.getId() );
			assertEquals( expected, actual, "Expected the correct ordering. Assets which do not have a symbol of type1 should come first (null first)" );
		} );
	}
}
