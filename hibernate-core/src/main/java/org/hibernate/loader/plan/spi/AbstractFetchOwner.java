/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.loader.plan.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.spi.ResultSetProcessingContext;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractFetchOwner extends AbstractPlanNode implements FetchOwner {
	private final String alias;
	private final LockMode lockMode;

	private List<Fetch> fetches;

	public AbstractFetchOwner(SessionFactoryImplementor factory, String alias, LockMode lockMode) {
		super( factory );
		this.alias = alias;
		if ( alias == null ) {
			throw new HibernateException( "alias must be specified" );
		}
		this.lockMode = lockMode;
	}

	public String getAlias() {
		return alias;
	}

	public LockMode getLockMode() {
		return lockMode;
	}

	@Override
	public void addFetch(Fetch fetch) {
		if ( fetch.getOwner() != this ) {
			throw new IllegalArgumentException( "Fetch and owner did not match" );
		}

		if ( fetches == null ) {
			fetches = new ArrayList<Fetch>();
		}

		fetches.add( fetch );
	}

	@Override
	public Fetch[] getFetches() {
		return fetches == null ? NO_FETCHES : fetches.toArray( new Fetch[ fetches.size() ] );
	}
}
