/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.BagPersistentAttribute;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ListPersistentAttribute;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.SetPersistentAttribute;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.criteria.JpaPath;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.UnknownPathException;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;


/**
 * Convenience base class for SqmFrom implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmFrom<O,T> extends AbstractSqmPath<T> implements SqmFrom<O,T> {
	private String alias;

	private SqmFrom<O,T> correlationParent;
	private List<SqmJoin<T,?>> joins;

	protected AbstractSqmFrom(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedNavigable,
			SqmFrom lhs,
			String alias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedNavigable, lhs, nodeBuilder );

		if ( lhs == null ) {
			throw new IllegalArgumentException( "LHS cannot be null" );
		}
		this.alias = alias;
	}

	/**
	 * Intended for use with {@link SqmRoot}
	 */
	protected AbstractSqmFrom(
			EntityDomainType<T> entityType,
			String alias,
			NodeBuilder nodeBuilder) {
		super(
				alias == null
						? new NavigablePath( entityType.getHibernateEntityName() )
						: new NavigablePath( entityType.getHibernateEntityName() + '(' + alias + ')' ),
				entityType,
				null,
				nodeBuilder
		);

		this.alias = alias;
	}

	@Override
	public String getExplicitAlias() {
		return alias;
	}

	@Override
	public void setExplicitAlias(String explicitAlias) {
		this.alias = explicitAlias;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		final NavigablePath subNavPath = getNavigablePath().append( name );
		return creationState.getProcessingStateStack().getCurrent().getPathRegistry().resolvePath(
				subNavPath,
				snp -> {
					final SqmPathSource<?> subSource = getReferencedPathSource().findSubPathSource( name );
					if ( subSource == null ) {
						throw UnknownPathException.unknownSubPath( this, name );
					}

					return subSource.createSqmPath( this, creationState );
				}
		);
	}

	@Override
	public boolean hasJoins() {
		return ! (joins == null || joins.isEmpty() );
	}

	@Override
	public List<SqmJoin<T,?>> getSqmJoins() {
		return joins == null ? Collections.emptyList() : Collections.unmodifiableList( joins );
	}

	@Override
	public void addSqmJoin(SqmJoin<T,?> join) {
		if ( joins == null ) {
			joins = new ArrayList<>();
		}
		joins.add( join );
	}

	@Override
	public void visitSqmJoins(Consumer<SqmJoin<T,?>> consumer) {
		if ( joins != null ) {
			joins.forEach( consumer );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA


	@Override
	public JpaPath getParentPath() {
		return getLhs();
	}

	@Override
	public SqmFrom<O,T> getCorrelationParent() {
		return correlationParent;
	}

	@Override
	public boolean isCorrelated() {
		return false;
	}

	@Override
	public SqmFrom<O,T> correlateTo(JpaSubQuery<T> subquery) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<Join<T, ?>> getJoins() {
		return (Set) getSqmJoins().stream()
				.filter( sqmJoin -> ! ( sqmJoin instanceof SqmAttributeJoin && ( (SqmAttributeJoin) sqmJoin ).isFetched() ) )
				.collect( Collectors.toSet() );
	}

	@Override
	public <A> SqmSingularJoin<T, A> join(SingularAttribute<? super T, A> attribute) {
		return join( attribute, JoinType.INNER );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <A> SqmSingularJoin<T, A> join(SingularAttribute<? super T, A> attribute, JoinType jt) {
		return buildSingularJoin( (SingularPersistentAttribute) attribute, SqmJoinType.from( jt ), false );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <A> SqmBagJoin<T,A> join(CollectionAttribute<? super T, A> attribute) {
		return join( attribute, JoinType.INNER );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmBagJoin join(CollectionAttribute attribute, JoinType jt) {
		return buildBagJoin( (BagPersistentAttribute) attribute, SqmJoinType.from( jt ), false );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmSetJoin join(SetAttribute attribute) {
		return join( attribute, JoinType.INNER );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmSetJoin join(SetAttribute attribute, JoinType jt) {
		return buildSetJoin( (SetPersistentAttribute) attribute, SqmJoinType.from( jt ), false );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmListJoin join(ListAttribute attribute) {
		return join( attribute, JoinType.INNER );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmListJoin join(ListAttribute attribute, JoinType jt) {
		return buildListJoin( (ListPersistentAttribute) attribute, SqmJoinType.from( jt ), false );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmMapJoin join(MapAttribute attribute) {
		return join( attribute, JoinType.INNER );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmMapJoin join(MapAttribute attribute, JoinType jt) {
		return buildMapJoin( (MapPersistentAttribute) attribute, SqmJoinType.from( jt ), false );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmAttributeJoin join(String attributeName) {
		return join( attributeName, JoinType.INNER );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmAttributeJoin join(String attributeName, JoinType jt) {
		final SqmPathSource<?> subPathSource = getReferencedPathSource().findSubPathSource( attributeName );

		return buildJoin( subPathSource, SqmJoinType.from( jt ), false );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmBagJoin joinCollection(String attributeName) {
		return joinCollection( attributeName, JoinType.INNER );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmBagJoin joinCollection(String attributeName, JoinType jt) {
		final SqmPathSource<?> joinedPathSource = getReferencedPathSource().findSubPathSource( attributeName );

		if ( joinedPathSource instanceof BagPersistentAttribute ) {
			return buildBagJoin( (BagPersistentAttribute) joinedPathSource, SqmJoinType.from( jt ), false );
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Passed attribute name [%s] did not correspond to a collection (bag) reference [%s] relative to %s",
						attributeName,
						joinedPathSource,
						getNavigablePath().getFullPath()
				)
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmSetJoin joinSet(String attributeName) {
		return joinSet( attributeName, JoinType.INNER );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmSetJoin joinSet(String attributeName, JoinType jt) {
		final SqmPathSource<?> joinedPathSource = getReferencedPathSource().findSubPathSource( attributeName );

		if ( joinedPathSource instanceof SetPersistentAttribute ) {
			return buildSetJoin( (SetPersistentAttribute) joinedPathSource, SqmJoinType.from( jt ), false );
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Passed attribute name [%s] did not correspond to a collection (set) reference [%s] relative to %s",
						attributeName,
						joinedPathSource,
						getNavigablePath().getFullPath()
				)
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmListJoin joinList(String attributeName) {
		return joinList( attributeName, JoinType.INNER );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmListJoin joinList(String attributeName, JoinType jt) {
		final SqmPathSource<?> joinedPathSource = getReferencedPathSource().findSubPathSource( attributeName );

		if ( joinedPathSource instanceof ListPersistentAttribute ) {
			return buildListJoin( (ListPersistentAttribute) joinedPathSource, SqmJoinType.from( jt ), false );
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Passed attribute name [%s] did not correspond to a collection (list) reference [%s] relative to %s",
						attributeName,
						joinedPathSource,
						getNavigablePath().getFullPath()
				)
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmMapJoin joinMap(String attributeName) {
		return joinMap( attributeName, JoinType.INNER );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmMapJoin joinMap(String attributeName, JoinType jt) {
		final SqmPathSource<?> joinedPathSource = getReferencedPathSource().findSubPathSource( attributeName );

		if ( joinedPathSource instanceof MapPersistentAttribute ) {
			return buildMapJoin( (MapPersistentAttribute) joinedPathSource, SqmJoinType.from( jt ), false );
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Passed attribute name [%s] did not correspond to a collection (map) reference [%s] relative to %s",
						attributeName,
						joinedPathSource,
						getNavigablePath().getFullPath()
				)
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<Fetch<T, ?>> getFetches() {
		return (Set) getSqmJoins().stream()
				.filter( sqmJoin -> sqmJoin instanceof SqmAttributeJoin && ( (SqmAttributeJoin) sqmJoin ).isFetched() )
				.collect( Collectors.toSet() );
	}

	@Override
	public <A> SqmSingularJoin<T,A> fetch(SingularAttribute<? super T, A> attribute) {
		return fetch( attribute, JoinType.INNER );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <A> SqmSingularJoin<T,A> fetch(SingularAttribute<? super T, A> attribute, JoinType jt) {
		return buildSingularJoin(
				(SingularPersistentAttribute) attribute,
				SqmJoinType.from( jt ),
				true
		);
	}

	@Override
	public <A> SqmAttributeJoin<T, A> fetch(PluralAttribute<? super T, ?, A> attribute) {
		return fetch( attribute, JoinType.INNER );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <A> SqmAttributeJoin<T, A> fetch(PluralAttribute<? super T, ?, A> attribute, JoinType jt) {
		return buildJoin(
				(PluralPersistentAttribute) attribute,
				SqmJoinType.from( jt ),
				true
		);
	}

	@Override
	public <X,A> SqmAttributeJoin<X,A> fetch(String attributeName) {
		return fetch( attributeName, JoinType.INNER );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X,A> SqmAttributeJoin<X,A> fetch(String attributeName, JoinType jt) {
		final SqmPathSource<?> fetchedPathSource = getReferencedPathSource().findSubPathSource( attributeName );
		return buildJoin( fetchedPathSource, SqmJoinType.from( jt ), true );
	}

	private <A> SqmAttributeJoin buildJoin(
			SqmPathSource<A> joinedPathSource,
			SqmJoinType joinType,
			boolean fetched) {
		if ( joinedPathSource instanceof SingularPersistentAttribute ) {
			return buildSingularJoin(
					(SingularPersistentAttribute<T,A>) joinedPathSource,
					joinType,
					fetched
			);
		}

		if ( joinedPathSource instanceof BagPersistentAttribute ) {
			return buildBagJoin(
					(BagPersistentAttribute) joinedPathSource,
					joinType,
					fetched
			);
		}

		if ( joinedPathSource instanceof ListPersistentAttribute ) {
			return buildListJoin(
					(ListPersistentAttribute) joinedPathSource,
					joinType,
					fetched
			);
		}

		if ( joinedPathSource instanceof MapPersistentAttribute ) {
			return buildMapJoin(
					(MapPersistentAttribute) joinedPathSource,
					joinType,
					fetched
			);
		}

		if ( joinedPathSource instanceof SetPersistentAttribute ) {
			return buildSetJoin(
					(SetPersistentAttribute) joinedPathSource,
					joinType,
					fetched
			);
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Passed attribute [%s] did not correspond to a joinable reference [%s] relative to %s",
						joinedPathSource.getPathName(),
						joinedPathSource,
						getNavigablePath().getFullPath()
				)
		);
	}

	private <A> SqmSingularJoin<T,A> buildSingularJoin(
			SingularPersistentAttribute<T,A> attribute,
			SqmJoinType joinType,
			boolean fetched) {
		if ( attribute.getSqmPathType() instanceof ManagedDomainType ) {
			//noinspection unchecked
			return new SqmSingularJoin(
					this,
					attribute,
					null,
					joinType,
					fetched,
					nodeBuilder()
			);
		}

		throw new SemanticException( "Attribute [" + attribute + "] is not joinable" );

	}

	@SuppressWarnings("unchecked")
	private <E> SqmBagJoin<T,E> buildBagJoin(
			BagPersistentAttribute attribute,
			SqmJoinType joinType,
			boolean fetched) {
		return new SqmBagJoin(
				this,
				attribute,
				null,
				joinType,
				fetched,
				nodeBuilder()
		);
	}

	@SuppressWarnings("unchecked")
	private <E> SqmListJoin<T,E> buildListJoin(
			ListPersistentAttribute attribute,
			SqmJoinType joinType,
			boolean fetched) {
		return new SqmListJoin(
				this,
				attribute,
				null,
				joinType,
				fetched,
				nodeBuilder()
		);
	}

	@SuppressWarnings("unchecked")
	private <K,V> SqmMapJoin<T,K,V> buildMapJoin(
			MapPersistentAttribute attribute,
			SqmJoinType joinType,
			boolean fetched) {
		return new SqmMapJoin(
				this,
				attribute,
				null,
				joinType,
				fetched,
				nodeBuilder()
		);
	}

	@SuppressWarnings("unchecked")
	private <E> SqmSetJoin<T,E> buildSetJoin(
			SetPersistentAttribute attribute,
			SqmJoinType joinType,
			boolean fetched) {
		return new SqmSetJoin(
				this,
				attribute,
				null,
				joinType,
				fetched,
				nodeBuilder()
		);
	}
}
