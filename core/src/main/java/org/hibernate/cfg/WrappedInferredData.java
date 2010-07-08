/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.cfg;

import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.util.StringHelper;

/**
 * @author Emmanuel Bernard
 */
public class WrappedInferredData implements PropertyData {
	private PropertyData wrappedInferredData;
	private String propertyName;

	public XClass getClassOrElement() throws MappingException {
		return wrappedInferredData.getClassOrElement();
	}

	public String getClassOrElementName() throws MappingException {
		return wrappedInferredData.getClassOrElementName();
	}

	public AccessType getDefaultAccess() {
		return wrappedInferredData.getDefaultAccess();
	}

	public XProperty getProperty() {
		return wrappedInferredData.getProperty();
	}

	public XClass getDeclaringClass() {
		return wrappedInferredData.getDeclaringClass();
	}

	public XClass getPropertyClass() throws MappingException {
		return wrappedInferredData.getPropertyClass();
	}

	public String getPropertyName() throws MappingException {
		return propertyName;
	}

	public String getTypeName() throws MappingException {
		return wrappedInferredData.getTypeName();
	}

	public WrappedInferredData(PropertyData inferredData, String suffix) {
		this.wrappedInferredData = inferredData;
		this.propertyName = StringHelper.qualify( inferredData.getPropertyName(), suffix );
	}
}
