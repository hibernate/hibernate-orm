/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.query.sqm.tree.SqmCopyContext;

/**
 * Contract representing a from clause.
 * <p/>
 * The parent/child bit represents sub-queries.  The child from clauses are only used for test assertions,
 * but are left here as it is most convenient to maintain them here versus another structure.
 *
 * @author Steve Ebersole
 */
public class SqmFromClause implements Serializable {
	private List<SqmRoot<?>> domainRoots;

	public SqmFromClause() {
	}

	public SqmFromClause(int expectedNumberOfRoots) {
		this.domainRoots = CollectionHelper.arrayList( expectedNumberOfRoots );
	}

	private SqmFromClause(SqmFromClause original, SqmCopyContext context) {
		if ( original.domainRoots != null ) {
			this.domainRoots = new ArrayList<>( original.domainRoots.size() );
			for ( SqmRoot<?> domainRoot : original.domainRoots ) {
				this.domainRoots.add( domainRoot.copy( context ) );
			}
		}
	}

	public SqmFromClause copy(SqmCopyContext context) {
		return new SqmFromClause( this, context );
	}

	/**
	 * Immutable view of the domain roots.  Use {@link #setRoots} or {@link #addRoot} to
	 * mutate the roots
	 */
	public List<SqmRoot<?>> getRoots() {
		return domainRoots == null ? Collections.emptyList() : Collections.unmodifiableList( domainRoots );
	}

	/**
	 * Inject the complete set of domain roots
	 */
	public void setRoots(List<SqmRoot<?>> domainRoots) {
		this.domainRoots = domainRoots;
	}

	/**
	 * Add roots incrementally
	 */
	public void addRoot(SqmRoot<?> root) {
		if ( domainRoots == null ) {
			domainRoots = new ArrayList<>();
		}

		domainRoots.add( root );
	}

	/**
	 * Visit the domain roots
	 */
	public void visitRoots(Consumer<SqmRoot<?>> consumer) {
		if ( domainRoots != null ) {
			domainRoots.forEach( consumer );
		}
	}

	public int getNumberOfRoots() {
		if ( domainRoots == null ) {
			return 0;
		}
		else {
			return domainRoots.size();
		}
	}
}
