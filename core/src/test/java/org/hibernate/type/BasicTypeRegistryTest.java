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
package org.hibernate.type;

import java.net.URL;
import java.util.UUID;

import junit.framework.TestCase;

import org.hibernate.type.descriptor.java.UrlTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class BasicTypeRegistryTest extends TestCase {
	private final BasicTypeRegistry registry = new BasicTypeRegistry();

	public void testOverriding() {
		BasicType type = registry.getRegisteredType( "uuid-binary" );
		assertSame( UUIDBinaryType.INSTANCE, type );
		type = registry.getRegisteredType( UUID.class.getName() );
		assertSame( UUIDBinaryType.INSTANCE, type );

		BasicType override = new UUIDCharType() {
			@Override
			protected boolean registerUnderJavaType() {
				return true;
			}
		};
		registry.register( override );
		type = registry.getRegisteredType( UUID.class.getName() );
		assertNotSame( UUIDBinaryType.INSTANCE, type );
		assertSame( override, type );
	}

	public void testExpanding() {
		BasicType type = registry.getRegisteredType( URL.class.getName() );
		assertNull( type );

		registry.register( UrlType.INSTANCE );
		type = registry.getRegisteredType( URL.class.getName() );
		assertNotNull( type );
		assertSame( UrlType.INSTANCE, type );
	}

	public static class UrlType extends AbstractSingleColumnStandardBasicType<URL> {
		public static final UrlType INSTANCE = new UrlType();

		public UrlType() {
			super( VarcharTypeDescriptor.INSTANCE, UrlTypeDescriptor.INSTANCE );
		}

		public String getName() {
			return "url";
		}

		@Override
		protected boolean registerUnderJavaType() {
			return true;
		}
	}
}
