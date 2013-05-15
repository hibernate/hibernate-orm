package org.hibernate.testing.sql;

import java.util.List;

public class ForeignKey extends Constraint {

	public List< Reference > columns = new OptionallyOrderedSet< Reference >();
	public Reference references;

	ForeignKey( SqlObject parent ) {
		super( parent );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.testing.sqlparser.Constraint#columns()
	 */
	@Override
	public List< Reference > columns() {
		return columns;
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

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.testing.sqlparser.Constraint#type()
	 */
	@Override
	public String type() {
		return "FOREIGN KEY";
	}
}
