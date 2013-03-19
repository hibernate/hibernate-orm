package org.hibernate.testing.sqlparser;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class Expression {

	public List< String > operators = new ArrayList< String >();
	public List< Object > operands = new ArrayList< Object >();

	private void operandToString( StringBuilder builder, int index ) {
		Object operand = index < operands.size() ? operands.get( index ) : "";
		if ( operand instanceof Select || operand instanceof Case ) {
			builder.append( "( " ).append( operand ).append( " )" );
		} else if ( operand instanceof List ) {
			Statement.listToStringInParentheses( builder, ( List< ? > ) operand );
		} else {
			builder.append( operand );
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder( "( " );
		int maxOperandsNdx = operands.size() - 1;
		for ( int ndx = 0, namesSize = operators.size(); ndx < namesSize || ndx < maxOperandsNdx; ndx++ ) {
			if ( maxOperandsNdx > 0 ) {
				operandToString( builder, ndx );
			}
			builder.append( ' ' ).append( ( ndx < operators.size() ? operators.get( ndx ) : "" ) ).append( ' ' );
		}
		operandToString( builder, Math.max( maxOperandsNdx, 0 ) );
		builder.append( " )" );
		return builder.toString();
	}
}
