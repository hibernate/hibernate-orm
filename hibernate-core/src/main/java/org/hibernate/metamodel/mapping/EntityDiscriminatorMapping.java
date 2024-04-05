/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;

/**
 * Details about the discriminator for an entity hierarchy.
 *
 * @implNote All {@linkplain EntityMappingType entity-mappings} within the
 * hierarchy share the same EntityDiscriminatorMapping instance.
 *
 * @see jakarta.persistence.DiscriminatorColumn
 * @see jakarta.persistence.DiscriminatorValue
 *
 * @author Steve Ebersole
 */
public interface EntityDiscriminatorMapping extends DiscriminatorMapping, FetchOptions {

	String DISCRIMINATOR_ROLE_NAME = "{discriminator}";
	String LEGACY_DISCRIMINATOR_NAME = "class";

	static boolean matchesRoleName(String name) {
		return DISCRIMINATOR_ROLE_NAME.equals( name )
			|| LEGACY_DISCRIMINATOR_NAME.equalsIgnoreCase( name );
	}

	@Override
	default String getPartName() {
		return DISCRIMINATOR_ROLE_NAME;
	}

	@Override
	default String getFetchableName() {
		return getPartName();
	}

	/**
	 * Is the discriminator defined by a physical column?
	 */
	boolean hasPhysicalColumn();

	@Override
	default int getFetchableKey() {
		return -2;
	}

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

}
