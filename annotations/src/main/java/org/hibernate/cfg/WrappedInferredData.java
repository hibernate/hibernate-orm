//$Id$
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

	public String getDefaultAccess() {
		return wrappedInferredData.getDefaultAccess();
	}

	public XProperty getProperty() {
		return wrappedInferredData.getProperty();
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
