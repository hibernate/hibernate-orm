//$Id: NotEmptyExpression.java 6661 2005-05-03 20:12:20Z steveebersole $
package org.hibernate.criterion;

/**
 * @author Gavin King
 */
public class NotEmptyExpression extends AbstractEmptinessExpression implements Criterion {

	protected NotEmptyExpression(String propertyName) {
		super( propertyName );
	}

	protected boolean excludeEmpty() {
		return true;
	}

}
