package org.hibernate.testing.sqlparser;

public class Aliasable {

	public Object name;
	public String alias;

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return alias == null ? name.toString() : name + " AS " + alias;
	}
}
