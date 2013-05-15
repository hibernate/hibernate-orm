package org.hibernate.testing.sql;

public class Call extends DdlStatement {

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
