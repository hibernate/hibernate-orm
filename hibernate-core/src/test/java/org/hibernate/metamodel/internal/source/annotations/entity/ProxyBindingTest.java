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
package org.hibernate.metamodel.internal.source.annotations.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Proxy;
import org.hibernate.metamodel.spi.binding.EntityBinding;

import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Tests for {@code o.h.a.Cache}.
 *
 * @author Hardy Ferentschik
 */
public class ProxyBindingTest extends BaseAnnotationBindingTestCase {
	@Test
	@Resources(annotatedClasses = ProxiedEntity.class)
	public void testProxyNoAttributes() {
		EntityBinding binding = getEntityBinding( ProxiedEntity.class );
		assertTrue( "Wrong laziness", binding.isLazy() );
		assertEquals(
				"Wrong proxy interface",
				ProxiedEntity.class.getName(),
				binding.getProxyInterfaceType().getName().toString()
		);
	}

	@Test
	@Resources(annotatedClasses = NoProxyEntity.class)
	public void testNoProxy() {
		EntityBinding binding = getEntityBinding( NoProxyEntity.class );
		assertTrue( "Wrong laziness", binding.isLazy() );
		assertEquals(
				"Wrong proxy interface",
				NoProxyEntity.class.getName(),
				binding.getProxyInterfaceType().getName().toString()
		);
	}

	@Test
	@Resources(annotatedClasses = ProxyDisabledEntity.class)
	public void testProxyDisabled() {
		EntityBinding binding = getEntityBinding( ProxyDisabledEntity.class );
		assertFalse( "Wrong laziness", binding.isLazy() );
		assertEquals( "Wrong proxy interface", null, binding.getProxyInterfaceType() );
	}

	@Test
	@Resources(annotatedClasses = ProxyInterfaceEntity.class)
	public void testProxyInterface() {
		EntityBinding binding = getEntityBinding( ProxyInterfaceEntity.class );
		assertTrue( "Wrong laziness", binding.isLazy() );
		assertEquals(
				"Wrong proxy interface",
				ProxyBindingTest.ProxyInterfaceEntity.class.getName(),
				binding.getProxyInterfaceType().getName().toString()
		);
	}

	@Entity
	class NoProxyEntity {
		@Id
		private int id;
	}

	@Entity
	@Proxy
	class ProxiedEntity {
		@Id
		private int id;
	}

	@Entity
	@Proxy(lazy = false)
	class ProxyDisabledEntity {
		@Id
		private int id;
	}

	@Entity
	@Proxy(proxyClass = ProxyInterfaceEntity.class)
	class ProxyInterfaceEntity {
		@Id
		private int id;
	}
}


