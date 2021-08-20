/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;

/**
 * @author Steve Ebersole
 */
public interface EntityDiscriminatorMapping extends VirtualModelPart, BasicValuedModelPart, FetchOptions {
	String ROLE_NAME = "{discriminator}";
	String LEGACY_HQL_ROLE_NAME = "class";

	static boolean matchesRoleName(String name) {
		return ROLE_NAME.equals( name ) || LEGACY_HQL_ROLE_NAME.equals( name );
	}

	@Override
	default String getPartName() {
		return ROLE_NAME;
	}

	@Override
	default String getFetchableName() {
		return getPartName();
	}

	String getConcreteEntityNameForDiscriminatorValue(Object value);

	@Override
	BasicFetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState);

	@Override
	default FetchOptions getMappedFetchOptions() {
		return this;
	}

	@Override
	default FetchStyle getStyle() {
		return FetchStyle.JOIN;
	}

	@Override
	default FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}

	@Override
	default Fetch resolveCircularFetch(
			NavigablePath fetchablePath,
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			DomainResultCreationState creationState) {
		// can never be circular
		return null;
	}
}
