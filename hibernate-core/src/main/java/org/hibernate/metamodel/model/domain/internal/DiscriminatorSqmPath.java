/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.PathException;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.IllegalPathUsageException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.sql.internal.DiscriminatorPathInterpretation;
import org.hibernate.query.sqm.sql.internal.SelfInterpretingSqmPath;
import org.hibernate.query.sqm.sql.internal.SqmPathInterpretation;
import org.hibernate.query.sqm.tree.domain.AbstractSqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * SqmPath specialization for an entity discriminator
 *
 * @author Steve Ebersole
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class DiscriminatorSqmPath extends AbstractSqmPath implements SelfInterpretingSqmPath {
	private final EntityDomainType entityDomainType;
	private final EntityMappingType entityDescriptor;

	protected DiscriminatorSqmPath(
			SqmPathSource referencedPathSource,
			SqmPath<?> lhs,
			EntityDomainType entityDomainType,
			EntityMappingType entityDescriptor,
			NodeBuilder nodeBuilder) {
		super( lhs.getNavigablePath().append( EntityDiscriminatorMapping.ROLE_NAME ), referencedPathSource, lhs, nodeBuilder );
		this.entityDomainType = entityDomainType;
		this.entityDescriptor = entityDescriptor;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		if ( ! entityDescriptor.hasSubclasses() ) {
			return walker.visitEntityTypeLiteralExpression( new SqmLiteralEntityType( entityDomainType, nodeBuilder() ) );
		}

		return walker.visitSelfInterpretingSqmPath( this );
	}

	@Override
	public SqmPathInterpretation<?> interpret(
			SqlAstCreationState sqlAstCreationState,
			SemanticQueryWalker sqmWalker,
			boolean jpaQueryComplianceEnabled) {
		assert entityDescriptor.hasSubclasses();

		final TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup( getLhs().getNavigablePath() );
		final EntityMappingType entityMapping = ( (EntityValuedModelPart) tableGroup.getModelPart() ).getEntityMappingType();

		return new DiscriminatorPathInterpretation( getNavigablePath(), entityMapping, tableGroup, sqlAstCreationState );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		// todo (6.0) : no idea
	}

	@Override
	public SemanticPathPart resolvePathPart(String name, boolean isTerminal, SqmCreationState creationState) {
		throw new IllegalPathUsageException( "Discriminator cannot be de-referenced" );
	}

	@Override
	public SqmTreatedPath treatAs(Class treatJavaType) throws PathException {
		throw new UnsupportedOperationException( "Cannot apply TREAT operator to discriminator path" );
	}

	@Override
	public SqmTreatedPath treatAs(EntityDomainType treatTarget) throws PathException {
		throw new UnsupportedOperationException( "Cannot apply TREAT operator to discriminator path" );
	}
}
