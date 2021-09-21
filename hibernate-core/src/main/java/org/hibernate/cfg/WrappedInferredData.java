/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;
import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.internal.util.StringHelper;

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
