/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
