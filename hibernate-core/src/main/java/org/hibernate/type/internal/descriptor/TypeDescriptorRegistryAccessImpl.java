/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.internal.descriptor;

import org.hibernate.type.spi.descriptor.java.JavaTypeDescriptorRegistry;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptorRegistry;
import org.hibernate.type.spi.descriptor.TypeDescriptorRegistryAccess;

/**
 * Implementation of the TypeDescriptorRegistryAccess that is created during mapping metadata parsing
 * and continued to be held as a SessionFactory/Metamodel delegate.
 *
 * @author Steve Ebersole
 */
public class TypeDescriptorRegistryAccessImpl implements TypeDescriptorRegistryAccess {
	private final JavaTypeDescriptorRegistry javaTypeDescriptorRegistry;
	private final SqlTypeDescriptorRegistry sqlTypeDescriptorRegistry;

	private boolean initialized = false;

	public TypeDescriptorRegistryAccessImpl() {
		this.javaTypeDescriptorRegistry = new JavaTypeDescriptorRegistry( this );
		this.sqlTypeDescriptorRegistry = new SqlTypeDescriptorRegistry( this );

		this.initialized = true;
	}

	@Override
	public JavaTypeDescriptorRegistry getJavaTypeDescriptorRegistry() {
		if ( !initialized ) {
			throw new IllegalStateException( "TypeDescriptorRegistryAccess initialization incomplete; not yet ready for access" );
		}
		return javaTypeDescriptorRegistry;
	}

	@Override
	public SqlTypeDescriptorRegistry getSqlTypeDescriptorRegistry() {
		if ( !initialized ) {
			throw new IllegalStateException( "TypeDescriptorRegistryAccess initialization incomplete; not yet ready for access" );
		}
		return sqlTypeDescriptorRegistry;
	}
}
