//$Id: EmptyExpression.java 6661 2005-05-03 20:12:20Z steveebersole $
package org.hibernate.criterion;

/**
 * @author Gavin King
 */
public class EmptyExpression extends AbstractEmptinessExpression implements Criterion {

	protected EmptyExpression(String propertyName) {
		super( propertyName );
	}

	protected boolean excludeEmpty() {
		return false;
	}

}
