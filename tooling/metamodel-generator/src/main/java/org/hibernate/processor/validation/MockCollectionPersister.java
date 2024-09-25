/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.validation;

import org.hibernate.FetchMode;
import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.ListType;
import org.hibernate.type.MapType;
import org.hibernate.type.Type;

import static org.hibernate.internal.util.StringHelper.root;

/**
 * @author Gavin King
 */
@SuppressWarnings("nullness")
public abstract class MockCollectionPersister implements QueryableCollection {

	private static final String[] ID_COLUMN = {"id"};
	private static final String[] INDEX_COLUMN = {"pos"};

	private final String role;
	private final MockSessionFactory factory;
	private final CollectionType collectionType;
	private final String ownerEntityName;
	private final Type elementType;

	public MockCollectionPersister(String role, CollectionType collectionType, Type elementType, MockSessionFactory factory) {
		this.role = role;
		this.collectionType = collectionType;
		this.elementType = elementType;
		this.factory = factory;
		this.ownerEntityName = root(role);
	}

	String getOwnerEntityName() {
		return ownerEntityName;
	}

	@Override
	public String getRole() {
		return role;
	}

	@Override
	public String getName() {
		return role;
	}

	@Override
	public CollectionType getCollectionType() {
		return collectionType;
	}

	@Override
	public EntityPersister getOwnerEntityPersister() {
		return factory.getMetamodel().entityPersister(ownerEntityName);
	}

	abstract Type getElementPropertyType(String propertyPath);

	@Override
	public Type toType(String propertyName) throws QueryException {
		if ("index".equals(propertyName)) {
			//this is what AbstractCollectionPersister does!
			//TODO: move it to FromElementType:626 or all
			//	  the way to CollectionPropertyMapping
			return getIndexType();
		}
		Type type = getElementPropertyType(propertyName);
		if (type==null) {
			throw new QueryException(elementType.getName()
					+ " has no mapped "
					+ propertyName);
		}
		else {
			return type;
		}
	}

	@Override
	public Type getKeyType() {
		return getOwnerEntityPersister().getIdentifierType();
	}

	@Override
	public Type getIndexType() {
		if (collectionType instanceof ListType) {
			return factory.getTypeConfiguration().getBasicTypeForJavaType(Integer.class);
		}
		else if (collectionType instanceof MapType) {
			//TODO!!! this is incorrect, return the correct key type
			return factory.getTypeConfiguration().getBasicTypeForJavaType(String.class);
		}
		else {
			return null;
		}
	}

	@Override
	public Type getElementType() {
		return elementType;
	}

	@Override
	public Type getIdentifierType() {
		return factory.getTypeConfiguration().getBasicTypeForJavaType(Long.class);
	}

	@Override
	public boolean hasIndex() {
		return getCollectionType() instanceof ListType
			|| getCollectionType() instanceof MapType;
	}

	@Override
	public EntityPersister getElementPersister() {
		if (elementType instanceof EntityType ) {
			return factory.getMetamodel()
					.entityPersister(elementType.getName());
		}
		else {
			return null;
		}
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	@Override
	public boolean isOneToMany() {
		return elementType instanceof EntityType;
	}

	@Override
	public String[] getCollectionSpaces() {
		return new String[] {role};
	}

	@Override
	public String getMappedByProperty() {
		return null;
	}

	@Override
	public String[] getIndexColumnNames() {
		return INDEX_COLUMN;
	}

	@Override
	public String[] getIndexColumnNames(String alias) {
		return INDEX_COLUMN;
	}

	@Override
	public String[] getIndexFormulas() {
		return null;
	}

	@Override
	public String[] getElementColumnNames(String alias) {
		return new String[] {""};
	}

	@Override
	public String[] getElementColumnNames() {
		return new String[] {""};
	}

	@Override
	public FetchMode getFetchMode() {
		return FetchMode.DEFAULT;
	}

	@Override
	public String getTableName() {
		return role;
	}

	@Override
	public String[] getKeyColumnNames() {
		return ID_COLUMN;
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public boolean consumesCollectionAlias() {
		return true;
	}

	@Override
	public String[] toColumns(String propertyName) {
		return new String[] {""};
	}

}
