package org.hibernate.orm.test.mapping.collections.custom.parameterized;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserCollectionType;

/**
 * Our Hibernate type-system extension for defining our specialized collection
 * contract.
 *
 * @author Holger Brands
 * @author Steve Ebersole
 */
public class DefaultableListType implements UserCollectionType, ParameterizedType {
	private String defaultValue;

	@Override
	@Nonnull
	public CollectionClassification getClassification() {
		return CollectionClassification.LIST;
	}

	@Override
	@Nonnull
	public Class<?> getCollectionClass() {
		return DefaultableList.class;
	}

	@Nonnull
	public Object instantiate(int anticipatedSize) {
		DefaultableListImpl list = anticipatedSize < 0 ? new DefaultableListImpl() : new DefaultableListImpl( anticipatedSize );
		list.setDefaultValue( defaultValue );
		return list;
	}

	@Override
	@Nonnull
	public PersistentCollection instantiate(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull CollectionPersister persister) {
		return new PersistentDefaultableList( session );
	}

	@Override
	@Nonnull
	public PersistentCollection wrap(@Nonnull SharedSessionContractImplementor session, @Nonnull Object collection) {
		return new PersistentDefaultableList( session, ( List ) collection );
	}

	@Override
	@Nonnull
	public Iterator getElementsIterator(@Nonnull Object collection) {
		return ( ( DefaultableList ) collection ).iterator();
	}

	@Override
	public boolean contains(@Nonnull Object collection, @Nullable Object entity) {
		return ( ( DefaultableList ) collection ).contains( entity );
	}

	@Override
	@Nullable
	public Object indexOf(@Nonnull Object collection, @Nonnull Object entity) {
		int index = ( ( DefaultableList ) collection ).indexOf( entity );
		return index >= 0 ? new Integer( index ) : null;
	}

	@Override
	@Nonnull
	public Object replaceElements(
			@Nonnull Object original,
			@Nonnull Object target,
			@Nonnull CollectionPersister persister,
			@Nullable Object owner,
			@Nonnull Map copyCache,
			@Nonnull SharedSessionContractImplementor session) {
		DefaultableList result = ( DefaultableList ) target;
		result.clear();
		result.addAll( ( DefaultableList ) original );
		return result;
	}

	@Override
	public void setParameterValues(@Nonnull Properties parameters) {
		defaultValue = parameters.getProperty( "default" );
	}
}
