package org.hibernate.orm.test.jpa.model;


/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class MySubclassEntity extends MyEntity {
	private String someSubProperty;

	public String getSomeSubProperty() {
		return someSubProperty;
	}

	public void setSomeSubProperty(String someSubProperty) {
		this.someSubProperty = someSubProperty;
	}
}
