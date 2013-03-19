package org.hibernate.testing.sqlparser;

public class Function extends Expression {

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if ( !operators.isEmpty() ) {
			builder.append( operators.get( 0 ) );
		}
		Statement.listToStringInParentheses( builder, operands );
		return builder.toString();
	}
}
