/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.property;

import org.hibernate.property.access.internal.PropertyAccessStrategyBasicImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class BasicPropertyAccessorTest {
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
			this.it = (it == null || String.class.isInstance( it ))
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
		var accessStrategy = PropertyAccessStrategyBasicImpl.INSTANCE;

		{
			final PropertyAccess access = accessStrategy.buildPropertyAccess( Duper.class, "it", true );
			assertThat( access.getGetter().getReturnTypeClass() ).isEqualTo( String.class );
			assertThat( access.getSetter().getMethod().getParameterTypes()[0] ).isEqualTo( Object.class );
		}

		{
			final PropertyAccess access = accessStrategy.buildPropertyAccess( Duper2.class, "it", true );
			assertThat( access.getGetter().getReturnTypeClass() ).isEqualTo( String.class );
			assertThat( access.getSetter().getMethod().getParameterTypes()[0] ).isEqualTo( String.class );
		}
	}
}
