//$Id$
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
