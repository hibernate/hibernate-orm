/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.query;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.criteria.JoinType;

import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@SuppressWarnings("rawtypes")
@TestForIssue(jiraKey = "HHH-11895")
public class ComponentQueryTest extends BaseEnversJPAFunctionalTestCase {

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

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Asset.class, Symbol.class, SymbolType.class };
	}

	private SymbolType type1;
	private SymbolType type2;

	private Asset asset1;
	private Asset asset2;
	private Asset asset3;

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

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

		em.getTransaction().commit();
	}

	@Test
	public void testSingleSymbolUsingIdentifier() {
		List actual = getAuditReader().createQuery().forEntitiesAtRevision( Asset.class, 1 ).traverseRelation( "singleSymbol", JoinType.INNER, "s" )
				.add( AuditEntity.property( "s", "identifier" ).eq( "X1" ) ).up().addProjection( AuditEntity.id() ).getResultList();
		assertEquals( "Expected only asset2 to be returned", Collections.singletonList( asset2.getId() ), actual );
	}

	@Test
	public void testMultiSymbolUsingIdentifier() {
		List actual = getAuditReader().createQuery().forEntitiesAtRevision( Asset.class, 1 ).traverseRelation( "multiSymbols", JoinType.INNER, "s" )
				.add( AuditEntity.property( "s", "identifier" ).like( "X%" ) ).up().addProjection( AuditEntity.id() ).addOrder( AuditEntity.id().asc() )
				.getResultList();
		List<Integer> expected = new ArrayList<>();
		Collections.addAll( expected, asset2.getId(), asset3.getId() );
		assertEquals( "Expected only the ids of the assets with symbol T1", expected, actual );
	}

	@Test
	public void testSingleSymbolUsingType() {
		List actual = getAuditReader().createQuery().forEntitiesAtRevision( Asset.class, 1 ).traverseRelation( "singleSymbol", JoinType.INNER, "s" )
				.add( AuditEntity.property( "s", "type" ).eq( type1 ) ).up().addProjection( AuditEntity.id() ).getResultList();
		assertEquals( "Expected only asset2 to be returned", Collections.singletonList( asset2.getId() ), actual );
	}

	@Test
	public void testMultiSymbolUsingType() {
		List actual = getAuditReader().createQuery().forEntitiesAtRevision( Asset.class, 1 ).traverseRelation( "multiSymbols", JoinType.INNER, "s" )
				.add( AuditEntity.property( "s", "type" ).eq( type2 ) ).up().addProjection( AuditEntity.id() ).getResultList();
		assertEquals( " Expected only asset3 to be returned", Collections.singletonList( asset3.getId() ), actual );
	}

	@Test
	public void testJoinOnSingleComponentAssociation() {
		List actual = getAuditReader().createQuery().forEntitiesAtRevision( Asset.class, 1 ).traverseRelation( "singleSymbol", JoinType.INNER, "s" )
				.traverseRelation( "type", JoinType.INNER, "t" )
				.add( AuditEntity.property( "t", "name" ).eq( "T1" ) ).up().up().addProjection( AuditEntity.id() ).getResultList();
		assertEquals( "Expected only asset2 to be returned", Collections.singletonList( asset2.getId() ), actual );
	}

	@Test
	public void testJoinOnMultiComponentAssociation() {
		List actual = getAuditReader().createQuery().forEntitiesAtRevision( Asset.class, 1 ).traverseRelation( "multiSymbols", JoinType.INNER, "s" )
				.traverseRelation( "type", JoinType.INNER, "t" )
				.add( AuditEntity.property( "t", "name" ).eq( "T2" ) ).up().up().addProjection( AuditEntity.id() ).getResultList();
		assertEquals( "Expected only asset3 to be returned", Collections.singletonList( asset3.getId() ), actual );
	}

	@Test
	public void testOrderingOnSingleComponent() {
		List actual = getAuditReader().createQuery().forEntitiesAtRevision( Asset.class, 1 ).addProjection( AuditEntity.id() )
				.traverseRelation( "singleSymbol", JoinType.LEFT, "s" ).addOrder( AuditEntity.property( "s", "identifier" ).asc() ).getResultList();
		List<Integer> expected = new ArrayList<>();
		Collections.addAll( expected, asset1.getId(), asset2.getId(), asset3.getId() );
		assertEquals( "Expected all assets in correct order", expected, actual );
	}

	@Test
	public void testOrderingOnMultiComponent() {
		List actual = getAuditReader().createQuery().forEntitiesAtRevision( Asset.class, 1 ).addProjection( AuditEntity.id() )
				.traverseRelation( "multiSymbols", JoinType.LEFT, "s" ).traverseRelation( "type", JoinType.LEFT, "t" )
				.addOrder( AuditEntity.property( "t", "name" ).asc() ).addOrder( AuditEntity.property( "s", "identifier" ).asc() ).getResultList();
		List<Integer> expected = new ArrayList<>();
		Collections.addAll( expected, asset1.getId(), asset2.getId(), asset3.getId(), asset3.getId() );
		assertEquals( "Expected all assets in correct order", expected, actual );
	}

	@Test
	public void testProjectionOnSingleComponentProperty() {
		List actual = getAuditReader().createQuery().forEntitiesAtRevision( Asset.class, 1 ).add( AuditEntity.id().eq( asset2.getId() ) )
				.traverseRelation( "singleSymbol", JoinType.INNER, "s" ).addProjection( AuditEntity.property( "s", "identifier" ) ).getResultList();
		assertEquals( "Expected the symbol identifier of asset2 to be returned", Collections.singletonList( "X1" ), actual );
	}

	@Test
	public void testProjectionOnMultiComponentProperty() {
		List actual = getAuditReader().createQuery().forEntitiesAtRevision( Asset.class, 1 ).add( AuditEntity.id().eq( asset2.getId() ) )
				.traverseRelation( "multiSymbols", JoinType.INNER, "s" ).addProjection( AuditEntity.property( "s", "identifier" ) ).getResultList();
		assertEquals( "Expected the symbol identifier of asset2 to be returned", Collections.singletonList( "X" ), actual );
	}

	@Test
	public void testFunctionOnSingleComponentProperty() {
		List actual = getAuditReader().createQuery().forEntitiesAtRevision( Asset.class, 1 ).add( AuditEntity.id().eq( asset2.getId() ) )
				.traverseRelation( "singleSymbol", JoinType.INNER, "s" )
				.addProjection( AuditEntity.function( "CONCAT", AuditEntity.property( "s", "identifier" ), "Z" ) ).getResultList();
		assertEquals( "Expecte the symbol identfier of asset2 concatenated with 'Z'", Collections.singletonList( "X1Z" ), actual );
	}

	@Test
	public void testFunctionOnMultiComponentProperty() {
		List actual = getAuditReader().createQuery().forEntitiesAtRevision( Asset.class, 1 ).add( AuditEntity.id().eq( asset2.getId() ) )
				.traverseRelation( "multiSymbols", JoinType.INNER, "s" )
				.addProjection( AuditEntity.function( "CONCAT", AuditEntity.property( "s", "identifier" ), "Z" ) ).getResultList();
		assertEquals( "Expecte the symbol identfier of asset2 concatenated with 'Z'", Collections.singletonList( "XZ" ), actual );
	}

}
