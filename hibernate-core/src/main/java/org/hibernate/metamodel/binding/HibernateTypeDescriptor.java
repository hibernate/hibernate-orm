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
package org.hibernate.metamodel.binding;

import java.util.Properties;

import org.hibernate.type.Type;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class HibernateTypeDescriptor {
	private String typeName;
	private Type explicitType;
	private Properties typeParameters;

	public String getTypeName() {
		return typeName;
	}

	/* package-protected */
	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public Type getExplicitType() {
		return explicitType;
	}

	/* package-protected */
	public void setExplicitType(Type explicitType) {
		this.explicitType = explicitType;
	}

	public Properties getTypeParameters() {
		return typeParameters;
	}

	/* package-protected */
	void setTypeParameters(Properties typeParameters) {
		this.typeParameters = typeParameters;
	}
}
