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
package org.hibernate.metamodel.source.internal.hbm;

import java.util.Collections;
import java.util.Map;

import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.spi.HibernateTypeSource;
import org.hibernate.metamodel.source.spi.JavaTypeDescriptorResolvable;

/**
 * @author Steve Ebersole
 */
public class HibernateTypeSourceImpl implements HibernateTypeSource, JavaTypeDescriptorResolvable {
	private final String name;
	private final Map<String, String> parameters;
	private JavaTypeDescriptor javaTypeDescriptor;

	public HibernateTypeSourceImpl(String name) {
		this( name, Collections.<String, String>emptyMap() );
	}

	public HibernateTypeSourceImpl(String name, Map<String, String> parameters) {
		this.name = name;
		this.parameters = parameters;
	}

	public HibernateTypeSourceImpl(JavaTypeDescriptor javaTypeDescriptor) {
		this( null, javaTypeDescriptor );
	}

	public HibernateTypeSourceImpl(String name, JavaTypeDescriptor javaTypeDescriptor) {
		this( name );
		this.javaTypeDescriptor = javaTypeDescriptor;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Map<String, String> getParameters() {
		return parameters;
	}

	@Override
	public JavaTypeDescriptor getJavaType() {
		return javaTypeDescriptor;
	}

	@Override
	public void resolveJavaTypeDescriptor(JavaTypeDescriptor descriptor) {
		if ( this.javaTypeDescriptor != null ) {
			if ( this.javaTypeDescriptor != descriptor ) {
				throw new IllegalStateException( "Attempt to resolve an already resolved JavaTypeDescriptor" );
			}
		}
		this.javaTypeDescriptor = descriptor;
	}
}
