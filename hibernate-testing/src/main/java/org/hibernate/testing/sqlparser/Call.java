package org.hibernate.testing.sqlparser;

public class Call extends Statement {

	public Function function;

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "CALL " + function;
	}
}
