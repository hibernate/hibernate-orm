/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.reflite.internal;

import java.util.Collection;

import org.hibernate.metamodel.reflite.spi.ClassDescriptor;
import org.hibernate.metamodel.reflite.spi.InterfaceDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class InternalJavaTypeDescriptor implements JavaTypeDescriptor {
	public abstract ClassDescriptor getSuperclass();
	public abstract Collection<InterfaceDescriptor> getInterfaces();

	@Override
	public boolean isAssignableFrom(JavaTypeDescriptor check) {
		if ( check == null ) {
			throw new IllegalArgumentException( "Descriptor to check cannot be null" );
		}

		if ( equals( check ) ) {
			return true;
		}

		//noinspection SimplifiableIfStatement
		if ( InternalJavaTypeDescriptor.class.isInstance( check ) ) {
			return ( (InternalJavaTypeDescriptor) check ).isAssignableTo( this );
		}

		return false;
	}

	public boolean isAssignableTo(InternalJavaTypeDescriptor check) {
		ClassDescriptor superClass = getSuperclass();
		if ( check.equals( superClass ) ) {
			return true;
		}
		if ( superClass instanceof InternalJavaTypeDescriptor ) {
			if ( ( (InternalJavaTypeDescriptor) superClass ).isAssignableTo( check ) ) {
				return true;
			}
		}

		final Collection<InterfaceDescriptor> interfaces = getInterfaces();
		if ( interfaces != null ) {
			for ( InterfaceDescriptor interfaceDescriptor : interfaces ) {
				if ( check.equals( interfaceDescriptor ) ) {
					return true;
				}
				if ( interfaceDescriptor instanceof InternalJavaTypeDescriptor ) {
					if ( ( (InternalJavaTypeDescriptor) interfaceDescriptor ).isAssignableTo( check ) ) {
						return true;
					}
				}
			}
		}

		return false;
	}
}
