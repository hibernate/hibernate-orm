/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeBasic;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierCompositeAggregated;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierCompositeNonAggregated;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierSimple;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.SqlSelectionGroup;
import org.hibernate.sql.results.spi.EntitySqlSelectionMappings;

/**
 * @author Steve Ebersole
 */
public class EntitySqlSelectionMappingsBuilder implements NavigableVisitationStrategy {
	public static EntitySqlSelectionMappings buildSqlSelectionMappings(
			EntityDescriptor entityDescriptor,
			QueryResultCreationContext creationContext) {
		EntitySqlSelectionMappingsBuilder strategy = new EntitySqlSelectionMappingsBuilder( creationContext );
		entityDescriptor.visitNavigables( strategy );
		return strategy.buildSqlSelectionMappings();
	}

	private final QueryResultCreationContext creationContext;

	private SqlSelection rowIdSqlSelection;
	private SqlSelectionGroup idSqlSelectionGroup;
	private SqlSelection discriminatorSqlSelection;
	private SqlSelection tenantDiscriminatorSqlSelection;
	private Map<PersistentAttribute,SqlSelectionGroup> attributeSqlSelectionGroupMap;

	protected EntitySqlSelectionMappingsBuilder(QueryResultCreationContext creationContext) {
		this.creationContext = creationContext;
	}

	protected QueryResultCreationContext getCreationContext() {
		return creationContext;
	}

	public EntitySqlSelectionMappings buildSqlSelectionMappings() {
		return new EntitySqlSelectionMappingsImpl(
				rowIdSqlSelection,
				idSqlSelectionGroup,
				discriminatorSqlSelection,
				tenantDiscriminatorSqlSelection,
				attributeSqlSelectionGroupMap
		);
	}




	@Override
	public void visitSimpleIdentifier(EntityIdentifierSimple identifier) {
		setIdSqlSelectionGroup( identifier.resolveSqlSelectionGroup( getCreationContext() ) );
	}

	// todo (6.0) : consider a singular, pluggable `HibernateExceptionFactory` that can be used to seamlessly handle JPA v. native-Hibernate requirements.
	//		Another option would be to migrate Hibernate to usage of the JPA exception requirements,
	//		still using `HibernateException` derivatives for cases not covered by JPA.
	//
	//		The initial comment ^^ is the safest solution as it would allow for continuing the
	// 		current user expectations.  However, this is a fairly large code change.

	protected void setIdSqlSelectionGroup(SqlSelectionGroup group) {
		if ( idSqlSelectionGroup != null ) {
			throw new HibernateException( "Multiple calls to set entity id SqlSelections" );
		}

		idSqlSelectionGroup = group;;
	}

	@Override
	public void visitAggregateCompositeIdentifier(EntityIdentifierCompositeAggregated identifier) {
		setIdSqlSelectionGroup( identifier.resolveSqlSelectionGroup( getCreationContext() ) );
	}

	@Override
	public void visitNonAggregateCompositeIdentifier(EntityIdentifierCompositeNonAggregated identifier) {
		setIdSqlSelectionGroup( identifier.resolveSqlSelectionGroup( getCreationContext() ) );
	}

	@Override
	public void visitDiscriminator(DiscriminatorDescriptor discriminator) {
		setDiscriminatorSqlSelection( discriminator.resolveSqlSelectionGroup( getCreationContext() ) );
	}

	protected void setDiscriminatorSqlSelection(SqlSelectionGroup sqlSelectionGroup) {
		if ( sqlSelectionGroup != null ) {
			if ( sqlSelectionGroup.getSqlSelections().size() > 1 ) {
				throw new HibernateException( "Attempting to set multiple SqlSelections for discriminator" );
			}
		}

		setDiscriminatorSqlSelection( sqlSelectionGroup.getSqlSelections().get( 0 ) );
	}

	protected void setDiscriminatorSqlSelection(SqlSelection discriminatorSqlSelection) {
		if ( discriminatorSqlSelection != null ) {
			throw new HibernateException( "Multiple calls to set entity discriminator SqlSelection" );
		}

		this.discriminatorSqlSelection = discriminatorSqlSelection;
	}

	@Override
	public void visitSingularAttributeBasic(SingularPersistentAttributeBasic attribute) {

	}

	@Override
	public void visitSingularAttributeEmbedded(SingularPersistentAttributeEmbedded attribute) {

	}

	@Override
	public void visitSingularAttributeEntity(SingularPersistentAttributeEntity attribute) {

	}

	@Override
	public void visitPluralAttribute(PluralPersistentAttribute attribute) {

	}
}
