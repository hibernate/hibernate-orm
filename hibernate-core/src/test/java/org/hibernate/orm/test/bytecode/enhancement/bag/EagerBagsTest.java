package org.hibernate.orm.test.bytecode.enhancement.bag;

import java.util.Collection;
import java.util.LinkedList;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertTrue;


@RunWith(BytecodeEnhancerRunner.class)
public class EagerBagsTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				EntityA.class,
				EntityB.class,
				EntityC.class,
				EntityD.class,
		};
	}

	@Test
	public void testIt() {
		inTransaction(
				session -> {
					EntityB b = new EntityB( 1l, "b" );

					EntityC c = new EntityC( 1l, "c" );
					EntityC c1 = new EntityC( 2l, "c1" );

					b.addAttribute( c );
					b.addAttribute( c1 );

					EntityB b1 = new EntityB( 2l, "b1" );

					EntityC c2 = new EntityC( 3l, "c2" );
					EntityC c3 = new EntityC( 4l, "c3" );
					EntityC c4 = new EntityC( 5l, "c4" );

					b1.addAttribute( c2 );
					b1.addAttribute( c3 );
					b1.addAttribute( c4 );

					EntityA a = new EntityA( 1l, "a" );

					a.addAttribute( b );
					a.addAttribute( b1 );

					session.save( c );
					session.save( c1 );
					session.save( c2 );
					session.save( c3 );
					session.save( c4 );

					session.save( b );
					session.save( b1 );

					session.save( a );
				}
		);

		inTransaction(
				session -> {
					EntityA entityA = session.find( EntityA.class, 1l );
					Collection<EntityB> attributes = entityA.attributes;
					assertThat( attributes.size() ).isEqualTo( 2 );

					boolean findB = false;
					boolean findB1 = false;
					for ( EntityB entityB : attributes ) {
						Collection<EntityC> entityCS = entityB.attributes;
						if ( entityB.getName().equals( "b" ) ) {
							assertThat( entityCS.size() ).isEqualTo( 2 );
							findB = true;
						}
						else {
							assertThat( entityCS.size() ).isEqualTo( 3 );
							findB1 = true;
						}
					}
					assertTrue( findB );
					assertTrue( findB1 );
				}
		);
	}

	@Entity(name = "EntityA")
	public static class EntityA {
		@Id
		private Long id;

		private String name;

		@OneToMany(fetch = FetchType.EAGER)
		Collection<EntityB> attributes = new LinkedList<>();

		public EntityA() {
		}

		public EntityA(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Collection<EntityB> getAttributes() {
			return attributes;
		}

		public void addAttribute(EntityB entityB) {
			attributes.add( entityB );
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB {
		@Id
		private Long id;

		private String name;

		@OneToMany(fetch = FetchType.EAGER)
		Collection<EntityC> attributes = new LinkedList<>();


		public EntityB() {
		}

		public EntityB(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Collection<EntityC> getAttributes() {
			return attributes;
		}

		public void addAttribute(EntityC entityC) {
			this.attributes.add( entityC );
		}
	}

	@Entity(name = "EntityC")
	public static class EntityC {
		@Id
		private Long id;

		private String name;

		public EntityC() {
		}

		public EntityC(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "EntityD")
	public static class EntityD {
		@Id
		private Long id;

		private String name;

		public EntityD() {
		}

		public EntityD(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
