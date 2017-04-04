/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.Collections;
import java.util.Map;

import org.hibernate.boot.jaxb.hbm.spi.TypeContainer;
import org.hibernate.boot.model.JavaTypeDescriptor;
import org.hibernate.boot.model.source.spi.HibernateTypeSource;
import org.hibernate.boot.model.source.spi.JavaTypeDescriptorResolvable;

/**
 * @author Steve Ebersole
 */
public class HibernateTypeSourceImpl implements HibernateTypeSource, JavaTypeDescriptorResolvable {
	private final String name;
	private final Map<String, String> parameters;
	private JavaTypeDescriptor javaTypeDescriptor;

	public HibernateTypeSourceImpl(TypeContainer typeContainer) {
		if ( typeContainer.getTypeAttribute() != null ) {
			name = typeContainer.getTypeAttribute();
			parameters = null;
		}
		else if ( typeContainer.getType() != null ) {
			name = typeContainer.getType().getName();
			parameters = Helper.extractParameters( typeContainer.getType().getConfigParameters() );
		}
		else {
			name = null;
			parameters = null;
		}
	}

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
