/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.CollectionType;
import org.hibernate.type.spi.ColumnMapping;
import org.hibernate.type.spi.Type;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionType extends AbstractTypeImpl implements CollectionType, TypeConfigurationAware {
	protected static final Size LEGACY_DICTATED_SIZE = new Size();
	protected static final Size LEGACY_DEFAULT_SIZE = new Size( 19, 2, 255, Size.LobMultiplier.NONE ); // to match legacy behavior

	private final String roleName;

	private TypeConfiguration typeConfiguration;
	private final ColumnMapping[] columnMappings;

	public AbstractCollectionType(
			String roleName) {
		this( roleName, null, null, CollectionComparator.INSTANCE );
	}

	public AbstractCollectionType(
			String roleName,
			JavaTypeDescriptor javaTypeDescriptor,
			MutabilityPlan mutabilityPlan,
			Comparator comparator) {
		super( javaTypeDescriptor, mutabilityPlan, comparator );
		this.roleName = roleName;
		this.columnMappings = new ColumnMapping[] {
				new ColumnMapping(
						null,
						LEGACY_DICTATED_SIZE,
						LEGACY_DEFAULT_SIZE
				)
		};
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public void setTypeConfiguration(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}

	@Override
	public ColumnMapping[] getColumnMappings() {
		return columnMappings;
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {

		if ( value == null ) {
			return "null";
		}

		if ( !getReturnedClass().isInstance( value ) && !PersistentCollection.class.isInstance( value ) ) {
			// its most likely the collection-key
			final CollectionPersister persister = getCollectionPersister();
			if ( persister.getKeyType().getReturnedClass().isInstance( value ) ) {
				return roleName + "#" + getCollectionPersister( ).getKeyType().toLoggableString( value, factory );
			}
			else {
				// although it could also be the collection-id
				if ( persister.getIdentifierType() != null
						&& persister.getIdentifierType().getReturnedClass().isInstance( value ) ) {
					return roleName + "#" + getCollectionPersister( ).getIdentifierType().toLoggableString(
							value,
							factory
					);
				}
			}
		}

		if ( !Hibernate.isInitialized( value ) ) {
			return "<uninitialized>";
		}
		else {
			return renderLoggableString( value, factory );
		}
	}

	@Override
	public Object nullSafeGet(
			ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		return null;
	}

	@Override
	public Object nullSafeGet(
			ResultSet rs, String name, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		return null;
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st, Object value, int index, boolean[] settable, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {

	}

	@Override
	public void nullSafeSet(
			PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {

	}

	@Override
	public Object hydrate(
			ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		return null;
	}

	@Override
	public Object resolve(
			Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return null;
	}

	@Override
	public Object semiResolve(
			Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return null;
	}

	@Override
	public Type getSemiResolvedType(SessionFactoryImplementor factory) {
		return null;
	}

	@Override
	public int getColumnSpan() {
		return 0;
	}

	@Override
	public boolean[] toColumnNullness(Object value) {
		return ArrayHelper.EMPTY_BOOLEAN_ARRAY;
	}

	@Override
	public boolean isSame(Object x, Object y) throws HibernateException {
		return isEqual(x, y );
	}

	@Override
	public boolean isEqual(Object x, Object y) throws HibernateException {
		return x == y
				|| ( x instanceof PersistentCollection && ( (PersistentCollection) x ).wasInitialized() && ( (PersistentCollection) x ).isWrapper( y ) )
				|| ( y instanceof PersistentCollection && ( (PersistentCollection) y ).wasInitialized() && ( (PersistentCollection) y ).isWrapper( x ) );
	}

	@Override
	public boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) throws HibernateException {
		return isEqual(x, y );
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Object deepCopy(Object value, SessionFactoryImplementor factory) {
		return value;
	}

	@Override
	public Object replace(
			Object original, Object target, SharedSessionContractImplementor session, Object owner, Map copyCache)
			throws HibernateException {
		return null;
	}

	@Override
	public Object replace(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map copyCache,
			ForeignKeyDirection foreignKeyDirection) throws HibernateException {
		return null;
	}

	@Override
	public Object assemble(
			Serializable cached, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return null;
	}

	@Override
	public Serializable disassemble(
			Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return null;
	}

	@Override
	public boolean isDirty(Object old, Object current, SharedSessionContractImplementor session)
			throws HibernateException {
		//collections don't dirty an unversioned parent entity

		// TODO: I don't really like this implementation; it would be better if
		// this was handled by searchForDirtyCollections()
		return !isSame( old, current );
	}

	@Override
	public boolean isDirty(
			Object oldState, Object currentState, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException {
		return isDirty(oldState, currentState, session);
	}

	@Override
	public boolean isModified(
			Object dbState, Object currentState, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException {
		return false;
	}

	@Override
	public int getHashCode(Object value) throws HibernateException {
		throw new UnsupportedOperationException( "cannot doAfterTransactionCompletion lookups on collections" );
	}

	@Override
	public int[] sqlTypes() throws MappingException {
		return ArrayHelper.EMPTY_INT_ARRAY;
	}

	@Override
	public CollectionPersister getCollectionPersister() {
		return typeConfiguration.findCollectionPersister( roleName );
	}


	@Override
	public Type getElementType() throws MappingException {
		return getCollectionPersister().getElementReference().getOrmType();
	}

	public static class CollectionComparator implements Comparator<Object> {
		public static final CollectionComparator INSTANCE = new  CollectionComparator();

		@Override
		public int compare(Object x, Object y) {
			return 0; // collections cannot be compared
		}
	}

	protected String renderLoggableString(Object value, SessionFactoryImplementor factory) throws HibernateException {
		final List<String> list = new ArrayList<>();
		Type elemType = getElementType(  );
		Iterator itr = getElementsIterator( value );
		while ( itr.hasNext() ) {
			list.add( elemType.toLoggableString( itr.next(), factory ) );
		}
		return list.toString();
	}

	protected Iterator getElementsIterator(Object collection) {
		return ( (Collection) collection ).iterator();
	}
}
