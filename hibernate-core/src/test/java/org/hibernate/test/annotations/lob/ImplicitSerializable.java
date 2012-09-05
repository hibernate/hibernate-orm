package org.hibernate.test.annotations.lob;

import java.io.Serializable;

import org.hibernate.test.annotations.lob.EntitySerialize.CommonSerializable;

/**
 * @author Janario Oliveira
 */
public class ImplicitSerializable implements Serializable, CommonSerializable {
	String defaultValue;
	String value;

	@Override
	public String getDefaultValue() {
		return defaultValue;
	}

	@Override
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
}
