/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.persistence.TemporalType;

import org.hibernate.boot.model.domain.spi.EmbeddedValueMappingImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractCollectionIndex;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.CollectionIndexEmbedded;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.TableReferenceJoinCollector;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.procedure.ParameterMisuseException;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionIndexReferenceEmbedded;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.produce.spi.TableReferenceContributor;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class CollectionIndexEmbeddedImpl<J>
		extends AbstractCollectionIndex<J>
		implements CollectionIndexEmbedded<J> {
	private final EmbeddedTypeDescriptor<J> embeddedDescriptor;

	public CollectionIndexEmbeddedImpl(
			PersistentCollectionDescriptor descriptor,
			IndexedCollection bootCollectionMapping,
			RuntimeModelCreationContext creationContext) {
		super( descriptor, bootCollectionMapping );

		this.embeddedDescriptor = creationContext.getRuntimeModelDescriptorFactory().createEmbeddedTypeDescriptor(
				(EmbeddedValueMappingImplementor) bootCollectionMapping.getIndex(),
				descriptor,
				null,
				NAVIGABLE_NAME,
				SingularPersistentAttribute.Disposition.NORMAL,
				creationContext
		);
	}

	@Override
	public EmbeddedTypeDescriptor<J> getEmbeddedDescriptor() {
		return embeddedDescriptor;
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return getEmbeddedDescriptor().findNavigable( navigableName );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		getEmbeddedDescriptor().visitNavigables( visitor );
	}

	@Override
	public EmbeddableJavaDescriptor<J> getJavaTypeDescriptor() {
		return getEmbeddedDescriptor().getJavaTypeDescriptor();
	}


	@Override
	public SimpleTypeDescriptor<?> getDomainTypeDescriptor() {
		return getEmbeddedDescriptor();
	}

	@Override
	public List<Column> getColumns() {
		return getEmbeddedDescriptor().collectColumns();
	}

	@Override
	public SqmNavigableReference createSqmExpression(
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmCreationContext creationContext) {
		return new SqmCollectionIndexReferenceEmbedded( (SqmPluralAttributeReference) containerReference );
	}

	@Override
	public AllowableParameterType resolveTemporalPrecision(TemporalType temporalType, TypeConfiguration typeConfiguration) {
		throw new ParameterMisuseException( "Cannot apply temporal precision to embeddable value" );
	}

	@Override
	public void visitFetchables(Consumer<Fetchable> fetchableConsumer) {
		getEmbeddedDescriptor().visitFetchables( fetchableConsumer );
	}

	@Override
	public void applyTableReferenceJoins(
			ColumnReferenceQualifier lhs,
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector) {
		getEmbeddedDescriptor().visitStateArrayContributors(
				contributor -> {
					if ( contributor instanceof TableReferenceContributor ) {
						( (TableReferenceContributor) contributor ).applyTableReferenceJoins( lhs, joinType, sqlAliasBase, joinCollector );
					}
				}
		);
	}

	@Override
	public void visitColumns(
			BiConsumer<SqlExpressableType, Column> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		getEmbeddedDescriptor().visitStateArrayContributors(
				contributor -> contributor.visitColumns( action, clause, typeConfiguration )
		);
	}

	@Override
	public Object unresolve(Object value, SharedSessionContractImplementor session) {
		final Object[] values = getEmbeddedDescriptor().getPropertyValues( value );
		getEmbeddedDescriptor().visitStateArrayContributors(
				contributor -> {
					final int index = contributor.getStateArrayPosition();
					values[index] = contributor.unresolve( values[index], session );
				}
		);
		return values;
	}

	@Override
	public void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			Clause clause,
			SharedSessionContractImplementor session) {
		assert value instanceof Object[];
		final Object[] subValues = (Object[]) value;

		getEmbeddedDescriptor().visitStateArrayContributors(
				stateArrayContributor -> {
					final Object subValue = subValues[stateArrayContributor.getStateArrayPosition()];
					stateArrayContributor.dehydrate(
							subValue,
							jdbcValueCollector,
							clause,
							session
					);
				}
		);
	}

	@Override
	public boolean hasNotNullColumns() {
		return getEmbeddedDescriptor().visitAndCollectStateArrayContributors( contributor -> !contributor.isNullable() )
				.stream()
				.anyMatch( value -> value == true );
	}
}
