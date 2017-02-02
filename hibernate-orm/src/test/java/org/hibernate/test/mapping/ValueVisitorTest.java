/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.mapping;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Array;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.List;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PrimitiveArray;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Set;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ValueVisitor;

import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author max
 */
public class ValueVisitorTest extends BaseUnitTestCase {

	private StandardServiceRegistry serviceRegistry;
	private MetadataBuildingContext metadataBuildingContext;

	@Before
	public void prepare() {
		serviceRegistry = new StandardServiceRegistryBuilder().build();
		metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
	}

	@After
	public void release() {
		StandardServiceRegistryBuilder.destroy( serviceRegistry );
	}

	@Test
	public void testProperCallbacks() {
		final MetadataImplementor metadata =
				(MetadataImplementor) new MetadataSources( serviceRegistry )
		.buildMetadata();
		final Table tbl = new Table();
		final RootClass rootClass = new RootClass( metadataBuildingContext );

		ValueVisitor vv = new ValueVisitorValidator();

		new Any( metadata, tbl ).accept( vv );
		new Array( metadata, rootClass ).accept( vv );
		new Bag( metadata, rootClass ).accept( vv );
		new Component( metadata, rootClass ).accept( vv );
		new DependantValue( metadata, tbl, null ).accept( vv );
		new IdentifierBag( metadata, rootClass ).accept( vv );
		new List( metadata, rootClass ).accept( vv );
		new ManyToOne( metadata, tbl ).accept( vv );
		new Map( metadata, rootClass ).accept( vv );
		new OneToMany( metadata, rootClass ).accept( vv );
		new OneToOne( metadata, tbl, rootClass ).accept( vv );
		new PrimitiveArray( metadata, rootClass ).accept( vv );
		new Set( metadata, rootClass ).accept( vv );
		new SimpleValue( metadata ).accept( vv );
	}

	static public class ValueVisitorValidator implements ValueVisitor {

		public Object accept(PrimitiveArray primitiveArray) {
			return validate( PrimitiveArray.class, primitiveArray );
		}

		public Object accept(Bag bag) {
			return validate( Bag.class, bag );
		}

		public Object accept(DependantValue value) {
			return validate( DependantValue.class, value );
		}

		private Object validate(Class expectedClass, Object visitee) {
			if ( !visitee.getClass().getName().equals( expectedClass.getName() ) ) {
				throw new IllegalStateException(
						visitee.getClass().getName()
								+ " did not call proper accept method. Was "
								+ expectedClass.getName()
				);
			}
			return null;
		}

		public Object accept(IdentifierBag bag) {
			return validate( IdentifierBag.class, bag );
		}

		public Object accept(List list) {
			return validate( List.class, list );
		}

		public Object accept(Map map) {
			return validate( Map.class, map );
		}

		public Object accept(Array list) {
			return validate( Array.class, list );
		}

		public Object accept(OneToMany many) {
			return validate( OneToMany.class, many );
		}

		public Object accept(Set set) {
			return validate( Set.class, set );
		}

		public Object accept(Any any) {
			return validate( Any.class, any );
		}

		public Object accept(SimpleValue value) {
			return validate( SimpleValue.class, value );
		}

		public Object accept(Component component) {
			return validate( Component.class, component );
		}

		public Object accept(ManyToOne mto) {
			return validate( ManyToOne.class, mto );
		}

		public Object accept(OneToOne oto) {
			return validate( OneToOne.class, oto );
		}

	}
}
