//$Id$
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
