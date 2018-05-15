/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.ToOne;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractCollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionElementEntity;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.TableReferenceJoinCollector;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReferenceEntity;
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
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class CollectionElementEntityImpl<J>
		extends AbstractCollectionElement<J>
		implements CollectionElementEntity<J> {

	private static final Logger log = Logger.getLogger( CollectionElementEntityImpl.class );

	private final ElementClassification elementClassification;
	private final EntityTypeDescriptor<J> entityDescriptor;

	private boolean fullyInitialized;
	private ForeignKey foreignKey;
	private Navigable foreignKeyTargetNavigable;


	public CollectionElementEntityImpl(
			PersistentCollectionDescriptor runtimeDescriptor,
			Collection bootDescriptor,
			ElementClassification elementClassification,
			RuntimeModelCreationContext creationContext) {
		super( runtimeDescriptor );
		this.elementClassification = elementClassification;

		this.entityDescriptor = resolveEntityDescriptor( elementClassification, bootDescriptor, creationContext );

		creationContext.registerNavigable( this, bootDescriptor );
	}

	private EntityTypeDescriptor<J> resolveEntityDescriptor(
			ElementClassification elementClassification,
			Collection mappingBinding,
			RuntimeModelCreationContext creationContext) {
		final String elementEntityName;
		if ( elementClassification == ElementClassification.MANY_TO_MANY ) {
			elementEntityName = ( (ToOne) mappingBinding.getElement() ).getReferencedEntityName();
		}
		else if ( elementClassification == ElementClassification.ONE_TO_MANY ) {
			elementEntityName = ( (OneToMany) mappingBinding.getElement() ).getReferencedEntityName();
		}
		else {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Unexpected collection element classification [%s] for an entity-valued element",
							elementClassification.name()
					)
			);
		}

		return creationContext.getInFlightRuntimeModel().findEntityDescriptor( elementEntityName );
	}

	@Override
	public boolean finishInitialization(Object bootReference, RuntimeModelCreationContext creationContext) {
		if ( ! fullyInitialized ) {
			try {
				final boolean done = tryFinishInitialize( (Collection) bootReference, creationContext );
				if ( ! done ) {
					return false;
				}
			}
			catch ( Exception e ) {
				log.debugf( e, "#finishInitialization threw exception : %s ", getNavigableRole().getFullPath() );
				return false;
			}

			fullyInitialized = true;
		}

		return true;
	}

	protected boolean tryFinishInitialize(
			Collection bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		final String mappedBy = bootDescriptor.getMappedByProperty();
		if ( StringHelper.isEmpty( mappedBy ) ) {
			foreignKeyTargetNavigable = getEntityDescriptor().getIdentifierDescriptor();
		}
		else {
			foreignKeyTargetNavigable = getEntityDescriptor().findNavigable( mappedBy );
		}
		assert foreignKeyTargetNavigable != null;

		this.foreignKey = creationContext.getDatabaseObjectResolver().resolveForeignKey( bootDescriptor.getForeignKey() );
		assert foreignKey != null;

		return true;
	}

	@Override
	public EntityTypeDescriptor<J> getEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public String getEntityName() {
		return getEntityDescriptor().getEntityName();
	}

	@Override
	public String getJpaEntityName() {
		return getEntityDescriptor().getJpaEntityName();
	}

	@Override
	public ElementClassification getClassification() {
		return elementClassification;
	}

	@Override
	public Table getPrimaryDmlTable() {
		// todo (6.0) : technically this needs to be the table that holds the FK columns on the `entityDescriptor` side (secondary table, inheritance table, etc)
		return entityDescriptor.getPrimaryTable();
	}

	@Override
	public SimpleTypeDescriptor getDomainTypeDescriptor() {
		return getEntityDescriptor();
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return getEntityDescriptor().findNavigable( navigableName );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		getEntityDescriptor().visitNavigables( visitor );
	}


	@Override
	public EntityJavaDescriptor<J> getJavaTypeDescriptor() {
		return getEntityDescriptor().getJavaTypeDescriptor();
	}

	@Override
	public void applyTableReferenceJoins(
			ColumnReferenceQualifier lhs,
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector) {
		getEntityDescriptor().applyTableReferenceJoins( lhs, joinType, sqlAliasBase, joinCollector );
	}

	@Override
	public SqmNavigableReference createSqmExpression(
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmCreationContext creationContext) {
		return new SqmCollectionElementReferenceEntity( (SqmPluralAttributeReference) containerReference );
	}

	@Override
	public void visitJdbcTypes(Consumer<SqlExpressableType> action, Clause clause, TypeConfiguration typeConfiguration) {
		visitColumns(
				(sqlExpressableType, column) -> action.accept( sqlExpressableType ),
				clause,
				typeConfiguration
		);
	}

	@Override
	public Object unresolve(Object value, SharedSessionContractImplementor session) {
		return getEntityDescriptor().getIdentifier( value, session );
	}

	@Override
	public void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			Clause clause,
			SharedSessionContractImplementor session) {
		getEntityDescriptor().getIdentifierDescriptor().dehydrate( value, jdbcValueCollector, clause, session );
	}

	@Override
	public void visitColumns(
			BiConsumer<SqlExpressableType, Column> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		// todo (6.0) : should we skip this for inverse collections?
		if ( getCollectionDescriptor().isInverse() ) {
			return;
		}

		if ( clause == Clause.INSERT || clause == Clause.UPDATE ) {
			return;
		}

		foreignKey.getColumnMappings().getTargetColumns().forEach(
				column -> action.accept( column.getExpressableType(), column )
		);
	}

	@Override
	public DomainResult createDomainResult(
			NavigableReference navigableReference,
			String resultVariable,
			DomainResultCreationState creationState, DomainResultCreationContext creationContext) {
		// delegate to the persister because here we are returning
		// 		the entities that make up the referenced collection's elements
		return getEntityDescriptor().createDomainResult(
				navigableReference,
				resultVariable,
				creationState, creationContext
		);
	}

	@Override
	public boolean isNullable() {
		return getCollectionDescriptor().getDescribedAttribute().isNullable();
	}

	@Override
	public void visitFetchables(Consumer<Fetchable> fetchableConsumer) {
		entityDescriptor.visitFetchables( fetchableConsumer );
	}
}
