package org.hibernate.annotations.common.test.reflection.java.generics;

import java.util.List;
import java.util.Map;

/**
 * @author Davide Marchignoli
 * @author Paolo Perrotta
 */
@TestAnnotation(name = "xyz")
public class Dad<T> extends Grandpa<List<T>, Integer> {

	static Integer staticField;

	T fieldProperty;

	public Map<Double, T> getCollectionProperty() {
		return null;
	}

	@TestAnnotation(name = "xyz")
	public Integer getMethodProperty() {
		return null;
	}

	public int getPrimitiveProperty() {
		return 0;
	}

	public boolean isPropertyStartingWithIs() {
		return false;
	}

	public int[] getPrimitiveArrayProperty() {
		return null;
	}

	public T[] getArrayProperty() {
		return null;
	}

	public List<T> getGenericCollectionProperty() {
		return null;
	}

	public List<String> getNongenericCollectionProperty() {
		return null;
	}

	public static Integer getStaticThing() {
		return null;
	}

	public String getLanguage() {
		return null;
	}
}
