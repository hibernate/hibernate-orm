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
		MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl();
		new Any( metadataBuildingContext, tbl ).accept( vv );
		new Array( metadataBuildingContext, rootClass ).accept( vv );
		new Bag( metadataBuildingContext, rootClass ).accept( vv );
		new Component( metadataBuildingContext, rootClass ).accept( vv );
		new DependantValue( metadataBuildingContext, tbl, null ).accept( vv );
		new IdentifierBag( metadataBuildingContext, rootClass ).accept( vv );
		new List( metadataBuildingContext, rootClass ).accept( vv );
		new ManyToOne( metadataBuildingContext, tbl ).accept( vv );
		new Map( metadataBuildingContext, rootClass ).accept( vv );
		new OneToMany( metadataBuildingContext, rootClass ).accept( vv );
		new OneToOne( metadataBuildingContext, tbl, rootClass ).accept( vv );
		new PrimitiveArray( metadataBuildingContext, rootClass ).accept( vv );
		new Set( metadataBuildingContext, rootClass ).accept( vv );
		new SimpleValue( metadataBuildingContext ).accept( vv );
	}

	static public class ValueVisitorValidator implements ValueVisitor {

		@Override
		public Object accept(PrimitiveArray primitiveArray) {
			return validate( PrimitiveArray.class, primitiveArray );
		}

		@Override
		public Object accept(Bag bag) {
			return validate( Bag.class, bag );
		}

		@Override
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

		@Override
		public Object accept(IdentifierBag bag) {
			return validate( IdentifierBag.class, bag );
		}

		@Override
		public Object accept(List list) {
			return validate( List.class, list );
		}

		@Override
		public Object accept(Map map) {
			return validate( Map.class, map );
		}

		@Override
		public Object accept(Array list) {
			return validate( Array.class, list );
		}

		@Override
		public Object accept(OneToMany many) {
			return validate( OneToMany.class, many );
		}

		@Override
		public Object accept(Set set) {
			return validate( Set.class, set );
		}

		@Override
		public Object accept(Any any) {
			return validate( Any.class, any );
		}

		@Override
		public Object accept(SimpleValue value) {
			return validate( SimpleValue.class, value );
		}

		@Override
		public Object accept(Component component) {
			return validate( Component.class, component );
		}

		@Override
		public Object accept(ManyToOne mto) {
			return validate( ManyToOne.class, mto );
		}

		@Override
		public Object accept(OneToOne oto) {
			return validate( OneToOne.class, oto );
		}

	}
}
