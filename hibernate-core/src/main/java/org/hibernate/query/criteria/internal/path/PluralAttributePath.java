/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.path;

import java.io.Serializable;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.Type;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.PathSource;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * Models a path for a {@link PluralAttribute} generally obtained from a
 * {@link javax.persistence.criteria.Path#get} call
 *
 * @author Steve Ebersole
 */
public class PluralAttributePath<X> extends AbstractPathImpl<X> implements Serializable {
	private final PluralAttribute<?,X,?> attribute;
	private final CollectionPersister persister;

	public PluralAttributePath(
			CriteriaBuilderImpl criteriaBuilder,
			PathSource source,
			PluralAttribute<?,X,?> attribute) {
		super( criteriaBuilder, attribute.getJavaType(), source );
		this.attribute = attribute;
		this.persister = resolvePersister( criteriaBuilder, attribute );
	}

	private CollectionPersister resolvePersister(CriteriaBuilderImpl criteriaBuilder, PluralAttribute attribute) {
		SessionFactoryImplementor sfi = criteriaBuilder.getEntityManagerFactory().getSessionFactory();
		return sfi.getCollectionPersister( resolveRole( attribute ) );
	}

	private String resolveRole(PluralAttribute attribute) {
		switch ( attribute.getDeclaringType().getPersistenceType() ) {
			case ENTITY: {
				return attribute.getDeclaringType().getJavaType().getName() + '.' + attribute.getName();
			}
			case MAPPED_SUPERCLASS: {
				// the attribute is declared in a mappedsuperclass
				if ( getPathSource().getModel().getBindableType() == Bindable.BindableType.ENTITY_TYPE ) {
					// the role will be assigned to the "nearest" EnityType subclass of the MappedSuperclassType
					EntityType entityTypeNearestDeclaringType = (EntityType) getPathSource().getModel();
					IdentifiableType superType = entityTypeNearestDeclaringType.getSupertype();
					IdentifiableType previousType = entityTypeNearestDeclaringType;
					while ( superType != attribute.getDeclaringType() ) {
						if ( superType == null ) {
							throw new IllegalStateException(
									String.format(
											"Cannot determine nearest EntityType extending mapped superclass [%s]; [%s] extends [%s], but supertype of [%s] is null",
											attribute.getDeclaringType().getJavaType().getName(),
											( (EntityType) getPathSource().getModel() ).getJavaType().getName(),
											previousType.getJavaType().getName(),
											previousType.getJavaType().getName()
									)
							);
						}
						if ( superType.getPersistenceType() == Type.PersistenceType.ENTITY ) {
							entityTypeNearestDeclaringType = (EntityType) superType;
						}
						previousType = superType;
						superType = superType.getSupertype();
					}
					return entityTypeNearestDeclaringType.getJavaType().getName() + '.' + attribute.getName();
				}
				else {
					throw new AssertionFailure(
							String.format(
									"Unexpected BindableType; expected [%s]; instead got [%s]",
									Bindable.BindableType.ENTITY_TYPE,
									getPathSource().getModel().getBindableType()
							)
					);
				}
			}
			case EMBEDDABLE: {
				StringBuilder role = new StringBuilder().append( '.' ).append( attribute.getName() );
				PathSource parentPath = getPathSource();
				while ( SingularAttributePath.class.isInstance( parentPath ) ) {
					final SingularAttributePath singularAttributePath = (SingularAttributePath) parentPath;
					role.insert( 0, '.' );
					role.insert( 1, singularAttributePath.getAttribute().getName() );
					parentPath = singularAttributePath.getPathSource();
				}
				if ( !Root.class.isInstance( parentPath ) ) {
					throw new AssertionFailure(
							String.format(
									"Unexpected parentPath; expected it to be [%s], but was [%s].",
									Root.class.getName(),
									parentPath.getClass().getName()
							)
					);
				}
				return role.insert( 0, ( (Root) parentPath ).getModel().getJavaType().getName() ).toString();
			}
			default:
				throw new AssertionFailure(
						String.format(
								"Unexpected PersistenceType: [%s]",
								attribute.getDeclaringType().getPersistenceType()
						)
				);
		}
	}

	public PluralAttribute<?,X,?> getAttribute() {
		return attribute;
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	public CollectionPersister getPersister() {
		return persister;
	}

	@Override
	protected boolean canBeDereferenced() {
		// cannot be dereferenced
		return false;
	}

	@Override
	protected Attribute locateAttributeInternal(String attributeName) {
		throw new IllegalArgumentException( "Plural attribute paths cannot be further dereferenced" );
	}

	public Bindable<X> getModel() {
		// the issue here is the parameterized type; X is the collection
		// type (Map, Set, etc) while the "bindable" for a collection is the
		// elements.
		//
		// TODO : throw exception instead?
		return null;
	}

	@Override
	public <T extends X> PluralAttributePath<T> treatAs(Class<T> treatAsType) {
		throw new UnsupportedOperationException(
				"Plural attribute path [" + getPathSource().getPathIdentifier() + '.'
						+ attribute.getName() + "] cannot be dereferenced"
		);
	}
}
