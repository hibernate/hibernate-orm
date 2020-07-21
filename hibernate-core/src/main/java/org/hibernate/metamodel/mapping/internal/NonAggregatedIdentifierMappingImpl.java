/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collection;
import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.metamodel.internal.AbstractCompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * A "non-aggregated" composite identifier.
 * <p>
 * This is an identifier mapped using JPA's {@link javax.persistence.MapsId} feature.
 *
 * @author Steve Ebersole
 * @apiNote Technically a MapsId id does not have to be composite; we still handle that this class however
 */
public class NonAggregatedIdentifierMappingImpl extends AbstractCompositeIdentifierMapping {

	private final List<SingularAttributeMapping> idAttributeMappings;

	public NonAggregatedIdentifierMappingImpl(
			EmbeddableMappingType embeddableDescriptor,
			EntityMappingType entityMapping,
			List<SingularAttributeMapping> idAttributeMappings,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			String rootTableName,
			String[] rootTableKeyColumnNames,
			Component bootCidDescriptor,
			Component bootIdClassDescriptor,
			MappingModelCreationProcess creationProcess) {
		// todo (6.0) : handle MapsId and IdClass
		super(
				attributeMetadataAccess,
				embeddableDescriptor,
				entityMapping,
				rootTableName,
				rootTableKeyColumnNames,
				creationProcess.getCreationContext().getSessionFactory()
		);
		this.idAttributeMappings = idAttributeMappings;
	}

	@Override
	public int getAttributeCount() {
		return idAttributeMappings.size();
	}

	@Override
	public Collection<SingularAttributeMapping> getAttributes() {
		return idAttributeMappings;
	}

	@Override
	public Object getIdentifier(Object entity, SharedSessionContractImplementor session) {
		return entity;
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		// nothing to do
	}
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EmbeddableValuedFetchable

	@Override
	public Expression toSqlExpression(
			TableGroup tableGroup,
			Clause clause,
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		return null;
	}

	@Override
	public String getSqlAliasStem() {
		return "id";
	}

	@Override
	public String getFetchableName() {
		return "id";
	}

	@Override
	public int getNumberOfFetchables() {
		return idAttributeMappings.size();
	}

}
