/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

/**
 * Base implementation of a criteria node.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractNode implements CriteriaNode {
	private final CriteriaNodeBuilder builder;

	protected AbstractNode(CriteriaNodeBuilder builder) {
		this.builder = builder;
	}

	@Override
	public CriteriaNodeBuilder nodeBuilder() {
		return builder;
	}
}
