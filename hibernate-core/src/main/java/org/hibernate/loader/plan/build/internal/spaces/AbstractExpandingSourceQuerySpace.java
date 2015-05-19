/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal.spaces;

import org.hibernate.loader.plan.build.spi.ExpandingQuerySpace;
import org.hibernate.loader.plan.build.spi.ExpandingQuerySpaces;
import org.hibernate.loader.plan.spi.Join;

/**
 * @author Gail Badner
 */
public abstract class AbstractExpandingSourceQuerySpace extends AbstractQuerySpace implements ExpandingQuerySpace {

	public AbstractExpandingSourceQuerySpace(
			String uid,
			Disposition disposition,
			ExpandingQuerySpaces querySpaces,
			boolean canJoinsBeRequired) {
		super( uid, disposition, querySpaces, canJoinsBeRequired );
	}

	@Override
	public void addJoin(Join join) {
		internalGetJoins().add( join );
	}

	@Override
	public ExpandingQuerySpaces getExpandingQuerySpaces() {
		return super.getExpandingQuerySpaces();
	}
}
