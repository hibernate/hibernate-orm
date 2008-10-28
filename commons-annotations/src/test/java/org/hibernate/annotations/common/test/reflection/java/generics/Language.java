//$Id$
package org.hibernate.annotations.common.test.reflection.java.generics;

/**
 * @author Emmanuel Bernard
 */
public interface Language<T> {
	T getLanguage();
}
