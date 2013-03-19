package org.hibernate.testing.sqlparser;

public class ForeignKey extends Constraint {

	public Name references;

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.testing.sqlparser.Constraint#type()
	 */
	@Override
	public String type() {
		return "FOREIGN KEY";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.testing.sqlparser.Constraint#toString()
	 */
	@Override
	public String toString() {
		return super.toString() + " REFERENCES " + references;
	}
}
