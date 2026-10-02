package org.hibernate.orm.test.metamodel;

import java.io.Serializable;

interface ILocalizable extends Serializable {
	String getValue();

	void setValue(String value);
}
