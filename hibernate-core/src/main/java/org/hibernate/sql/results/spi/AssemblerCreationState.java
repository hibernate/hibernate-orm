/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;

/**
 * Access to information defining how {@link DomainResultAssembler}
 * instances should be created.
 *
 * @see DomainResult#createResultAssembler
 *
 * @author Steve Ebersole
 */
public interface AssemblerCreationState {
	default ColumnReferenceQualifier getCurrentColumnReferenceQualifier() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	default SqlExpressionResolver getSqlExpressionResolver() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	default LockOptions getLockOptions() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	default LoadQueryInfluencers getLoadQueryInfluencers() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	default boolean shouldCreateShallowEntityResult() {
		return false;
	}
}
