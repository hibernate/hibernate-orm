/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class BasicPropertyAccessorTest extends BaseUnitTestCase {
	public static abstract class Super {
		public abstract Object getIt();
		public abstract void setIt(Object it);
	}

	public static class Duper extends Super {
		private String it;

		public Duper(String it) {
			this.it = it;
		}

		public String getIt() {
			return it;
		}

		@Override
		public void setIt(Object it) {
			this.it = ( it == null || String.class.isInstance( it ) )
					? (String) it
					: it.toString();
		}
	}

	public static class Duper2 extends Super {
		private String it;

		public Duper2(String it) {
			this.it = it;
		}

		public String getIt() {
			return it;
		}

		public void setIt(String it) {
			this.it = it;
		}

		@Override
		public void setIt(Object it) {
			if ( it == null || String.class.isInstance( it ) ) {
				setIt( (String) it );
			}
			else {
				setIt( it.toString() );
			}
		}
	}

	@Test
	public void testBridgeMethodDisregarded() {
		BasicPropertyAccessor accessor = new BasicPropertyAccessor();

		{
			BasicPropertyAccessor.BasicGetter getter = (BasicPropertyAccessor.BasicGetter) accessor.getGetter( Duper.class, "it" );
			assertEquals( String.class, getter.getReturnType() );

			BasicPropertyAccessor.BasicSetter setter = (BasicPropertyAccessor.BasicSetter) accessor.getSetter( Duper.class, "it" );
			assertEquals( Object.class, setter.getMethod().getParameterTypes()[0] );
		}

		{
			BasicPropertyAccessor.BasicGetter getter = (BasicPropertyAccessor.BasicGetter) accessor.getGetter( Duper2.class, "it" );
			assertEquals( String.class, getter.getReturnType() );

			BasicPropertyAccessor.BasicSetter setter = (BasicPropertyAccessor.BasicSetter) accessor.getSetter( Duper2.class, "it" );
			assertEquals( String.class, setter.getMethod().getParameterTypes()[0] );
		}
	}
}
