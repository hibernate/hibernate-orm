/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal.spaces;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.build.spi.ExpandingQuerySpaces;
import org.hibernate.loader.plan.spi.Join;
import org.hibernate.loader.plan.spi.QuerySpace;
import org.hibernate.loader.plan.spi.QuerySpaces;

/**
 * Convenience base class for QuerySpace implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractQuerySpace implements QuerySpace {
	private final String uid;
	private final Disposition disposition;
	private final ExpandingQuerySpaces querySpaces;
	private final boolean canJoinsBeRequired;

	private List<Join> joins;

	public AbstractQuerySpace(
			String uid,
			Disposition disposition,
			ExpandingQuerySpaces querySpaces,
			boolean canJoinsBeRequired) {
		this.uid = uid;
		this.disposition = disposition;
		this.querySpaces = querySpaces;
		this.canJoinsBeRequired = canJoinsBeRequired;
	}

	protected SessionFactoryImplementor sessionFactory() {
		return querySpaces.getSessionFactory();
	}

	/**
	 * Can any joins created from here (with this as the left-hand side) be required joins?
	 *
	 * @return {@code true} indicates joins can be required; {@code false} indicates they cannot.
	 */
	public boolean canJoinsBeRequired() {
		return canJoinsBeRequired;
	}

	/**
	 * Provides subclasses access to the spaces to which this space belongs.
	 *
	 * @return The query spaces
	 */
	public QuerySpaces getQuerySpaces() {
		return querySpaces;
	}

	protected ExpandingQuerySpaces getExpandingQuerySpaces() {
		return querySpaces;
	}

	@Override
	public String getUid() {
		return uid;
	}

	@Override
	public Disposition getDisposition() {
		return disposition;
	}

	@Override
	public Iterable<Join> getJoins() {
		return joins == null
				? Collections.<Join>emptyList()
				: joins;
	}

	protected List<Join> internalGetJoins() {
		if ( joins == null ) {
			joins = new ArrayList<Join>();
		}

		return joins;
	}
}
