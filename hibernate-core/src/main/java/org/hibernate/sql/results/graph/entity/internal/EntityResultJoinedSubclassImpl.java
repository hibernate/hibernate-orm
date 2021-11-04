/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.entity.EntityInitializer;

/**
 * @author Andrea Boriero
 */
public class EntityResultJoinedSubclassImpl extends EntityResultImpl {
	public EntityResultJoinedSubclassImpl(
			NavigablePath navigablePath,
			EntityValuedModelPart entityValuedModelPart,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		super( navigablePath, entityValuedModelPart, tableGroup, resultVariable, creationState );
	}

	@Override
	public DomainResultAssembler createResultAssembler(AssemblerCreationState creationState) {
		final EntityInitializer initializer = (EntityInitializer) creationState.resolveInitializer(
				getNavigablePath(),
				getReferencedModePart(),
				() -> new EntityResultInitializer(
						this,
						getNavigablePath(),
						getLockMode( creationState ),
						getIdentifierFetch(),
						getDiscriminatorFetch(),
						getRowIdResult(),
						creationState
				)
		);

		return new EntityAssembler( getResultJavaTypeDescriptor(), initializer );
	}

}
