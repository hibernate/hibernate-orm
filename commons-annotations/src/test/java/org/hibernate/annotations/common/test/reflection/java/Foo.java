package org.hibernate.annotations.common.test.reflection.java;

import java.util.List;

/**
 * @author Paolo Perrotta
 */
@TestAnnotation(name = "xyz")
public class Foo extends FooFather {

	public static Integer staticField;

	Integer fieldProperty;

	public List<String> getCollectionProperty() {
		return null;
	}

	@TestAnnotation(name = "xyz")
	public Integer getMethodProperty() {
		return null;
	}

	public int getPrimitiveProperty() {
		return 0;
	}

	public static Integer getStaticThing() {
		return null;
	}
}
