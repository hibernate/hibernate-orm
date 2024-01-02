/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.Incubating;
import org.hibernate.annotations.Table;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;

/**
 * Anything that can be the target of {@linkplain MutationExecutor mutations}
 *
 * @author Steve Ebersole
 */
@Incubating
public interface EntityMutationTarget extends MutationTarget<EntityTableMapping> {

	@Override
	EntityMappingType getTargetPart();

	@Override
	EntityTableMapping getIdentifierTableMapping();

	/**
	 * The ModelPart describing the identifier/key for this target
	 */
	ModelPart getIdentifierDescriptor();

	/**
	 * Whether this target defines any potentially skippable tables.
	 * <p>
	 * A table is considered potentially skippable if it is defined
	 * as inverse or as optional.
	 *
	 * @see Table#inverse
	 * @see Table#optional
	 */
	boolean hasSkippableTables();

	/**
	 * The delegate for executing inserts against the root table for
	 * targets defined using post-insert id generation
	 *
	 * @deprecated use {@link #getInsertDelegate()} instead
	 */
	@Deprecated( forRemoval = true, since = "6.5" )
	default InsertGeneratedIdentifierDelegate getIdentityInsertDelegate() {
		final GeneratedValuesMutationDelegate insertDelegate = getInsertDelegate();
		if ( insertDelegate instanceof InsertGeneratedIdentifierDelegate ) {
			return (InsertGeneratedIdentifierDelegate) insertDelegate;
		}
		return null;
	}

	GeneratedValuesMutationDelegate getInsertDelegate();

	GeneratedValuesMutationDelegate getUpdateDelegate();

	default GeneratedValuesMutationDelegate getMutationDelegate(MutationType mutationType) {
		switch ( mutationType ) {
			case INSERT:
				return getInsertDelegate();
			case UPDATE:
				return getUpdateDelegate();
			default:
				return null;
		}
	}
}
