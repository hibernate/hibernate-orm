/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;

/**
 * `select p1.address, p2.address from Person p1, Person p2`
 *
 * table PERSON (
 *     ID,
 *     NAME,
 *     ...
 * )
 *
 * table Address (
 *     ID,
 *     PERS_ID,
 *     ...
 * )
 *
 * QueryResult (p1)
 *     Fetch(p1.address)
 * QueryResult(p2)
 *     Fetch(p2.address)
 *
 * select p1, p2
 * from Person p1 join fetch p1.address,
 * 	 Person p2 join fetch p2.address
 *
 * SELECT a1.*, a2.*
 * FROM (person p1 join address a1 on p1.id = a1.pers_id)
 *     join (person p2 join address a2 on p2.id = a2.pers_id)
 *
 * @author Steve Ebersole
 */
public class SqmSingularAttributeReferenceEntity
		extends AbstractSqmSingularAttributeReference
		implements SqmEntityTypedReference {
	private final SqmNavigableJoin navigableJoin;

	public SqmSingularAttributeReferenceEntity(
			SqmNavigableContainerReference containerReference,
			SingularPersistentAttributeEntity referencedAttribute,
			SqmCreationContext creationContext) {
		super( containerReference, referencedAttribute );
		this.navigableJoin = creationContext.getCurrentFromElementBuilder()
				.buildNavigableJoin( this );
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return navigableJoin;
	}

	@Override
	public SingularPersistentAttributeEntity getReferencedNavigable() {
		return (SingularPersistentAttributeEntity) super.getReferencedNavigable();
	}

	@Override
	public EntityValuedExpressableType getExpressableType() {
		return (EntityValuedExpressableType) super.getExpressableType();
	}

	@Override
	public EntityValuedExpressableType getInferableType() {
		return getExpressableType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitEntityValuedSingularAttribute( this );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return super.getExpressableType().getPersistenceType();
	}
}
