/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

/**
 * @author Emmanuel Bernard
 */
public class WrappedInferredData implements PropertyData {
	private final PropertyData wrappedInferredData;
	private final String propertyName;

	public WrappedInferredData(PropertyData inferredData, String suffix) {
		this.wrappedInferredData = inferredData;
		this.propertyName = StringHelper.qualify( inferredData.getPropertyName(), suffix );
	}

	@Override
	public TypeDetails getClassOrElementType() throws MappingException {
		return wrappedInferredData.getClassOrElementType();
	}

	@Override
	public XClass getClassOrPluralElement() throws MappingException {
		return wrappedInferredData.getClassOrPluralElement();
	}

	@Override
	public String getClassOrElementName() throws MappingException {
		return wrappedInferredData.getClassOrElementName();
	}

	@Override
	public AccessType getDefaultAccess() {
		return wrappedInferredData.getDefaultAccess();
	}

	@Override
	public MemberDetails getAttributeMember() {
		return wrappedInferredData.getAttributeMember();
	}

	@Override
	public ClassDetails getDeclaringClass() {
		return wrappedInferredData.getDeclaringClass();
	}

	@Override
	public TypeDetails getPropertyType() throws MappingException {
		return wrappedInferredData.getPropertyType();
	}

	@Override
	public String getPropertyName() throws MappingException {
		return propertyName;
	}

	@Override
	public String getTypeName() throws MappingException {
		return wrappedInferredData.getTypeName();
	}
}
