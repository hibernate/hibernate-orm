/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.internal.AbstractCompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;

/**
 * Support for {@link javax.persistence.EmbeddedId}
 *
 * @author Andrea Boriero
 */
public class EmbeddedIdentifierMappingImpl extends AbstractCompositeIdentifierMapping {
	private final String name;
	private final PropertyAccess propertyAccess;

	@SuppressWarnings("WeakerAccess")
	public EmbeddedIdentifierMappingImpl(
			EntityMappingType entityMapping,
			String name,
			EmbeddableMappingType embeddableDescriptor,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			PropertyAccess propertyAccess,
			String tableExpression,
			String[] attrColumnNames,
			SessionFactoryImplementor sessionFactory) {
		super(
				attributeMetadataAccess,
				embeddableDescriptor,
				entityMapping,
				tableExpression,
				attrColumnNames,
				sessionFactory
		);

		this.name = name;
		this.propertyAccess = propertyAccess;
	}

	@Override
	public String getPartName() {
		return name;
	}

	@Override
	public Object getIdentifier(Object entity, SharedSessionContractImplementor session) {
		if ( entity instanceof HibernateProxy ) {
			return ( (HibernateProxy) entity ).getHibernateLazyInitializer().getIdentifier();
		}
		return propertyAccess.getGetter().get( entity );
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		propertyAccess.getSetter().set( entity, id, session.getFactory() );
	}

	@Override
	public void visitJdbcValues(
			Object value,
			Clause clause,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		getEmbeddableTypeDescriptor().getAttributeMappings().forEach(
				attributeMapping -> {
					final Object o = attributeMapping.getPropertyAccess().getGetter().get( value );
					if ( attributeMapping instanceof ToOneAttributeMapping ) {
						final EntityMappingType associatedEntityMappingType =
								( (ToOneAttributeMapping) attributeMapping ).getAssociatedEntityMappingType();
						final EntityIdentifierMapping identifierMapping =
								associatedEntityMappingType.getIdentifierMapping();
						final Object identifier = identifierMapping.getIdentifier( o, session );
						identifierMapping.visitJdbcValues( identifier, clause, valuesConsumer, session );
					}
					else {
						attributeMapping.visitJdbcValues( o, clause, valuesConsumer, session );
					}
				}
		);
	}

	@Override
	public Expression toSqlExpression(
			TableGroup tableGroup,
			Clause clause,
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		final List<String> attrColumnNames = getMappedColumnExpressions();
		final List<ColumnReference> columnReferences = CollectionHelper.arrayList( attrColumnNames.size() );
		final TableReference tableReference = tableGroup.resolveTableReference( getContainingTableExpression() );
		getEmbeddableTypeDescriptor().visitJdbcTypes(
				new Consumer<JdbcMapping>() {
					private int index = 0;

					@Override
					public void accept(JdbcMapping jdbcMapping) {
						final String attrColumnExpr = attrColumnNames.get( index++ );

						final Expression columnReference = sqlAstCreationState.getSqlExpressionResolver()
								.resolveSqlExpression(
										SqlExpressionResolver.createColumnReferenceKey(
												tableReference,
												attrColumnExpr
										),
										sqlAstProcessingState -> new ColumnReference(
												tableReference.getIdentificationVariable(),
												attrColumnExpr,
												false,
												jdbcMapping,
												sqlAstCreationState.getCreationContext().getSessionFactory()
										)
								);

						columnReferences.add( (ColumnReference) columnReference );
					}
				},
				clause,
				sqlAstCreationState.getCreationContext().getSessionFactory().getTypeConfiguration()
		);

		return new SqlTuple( columnReferences, this );
	}

	@Override
	public String getSqlAliasStem() {
		return name;
	}


	@Override
	public String getFetchableName() {
		return name;
	}


	@Override
	public int getNumberOfFetchables() {
		return getEmbeddableTypeDescriptor().getNumberOfAttributeMappings();
	}


	@Override
	public int getAttributeCount() {
		return getEmbeddableTypeDescriptor().getNumberOfAttributeMappings();
	}

	@Override
	public Collection<SingularAttributeMapping> getAttributes() {
		//noinspection unchecked
		return (Collection) getEmbeddableTypeDescriptor().getAttributeMappings();
	}

}
