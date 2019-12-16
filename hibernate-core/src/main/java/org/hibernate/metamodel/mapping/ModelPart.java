/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.function.BiConsumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Describes a mapping related to any part of the app's domain model - e.g.
 * an attribute, an entity identifier, collection elements, etc
 *
 * @see DomainResultProducer
 * @see javax.persistence.metamodel.Bindable
 *
 * @author Steve Ebersole
 */
public interface ModelPart extends MappingModelExpressable {
	MappingType getPartMappingType();

	JavaTypeDescriptor getJavaTypeDescriptor();

	String getPartName();

	/**
	 * Create a DomainResult for a specific reference to this ModelPart.
	 */
	default <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * Apply SQL selections for a specific reference to this ModelPart outside the domain query's root select clause.
	 */
	default void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * Apply SQL selections for a specific reference to this ModelPart outside the domain query's root select clause.
	 */
	default void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection,JdbcMapping> selectionConsumer) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	default void visitColumns(ColumnConsumer consumer) {

	}

}
