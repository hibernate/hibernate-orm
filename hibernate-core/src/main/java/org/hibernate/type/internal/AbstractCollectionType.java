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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
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

	public AbstractCollectionType(String roleName, Comparator comparator) {
		this( roleName, null, null, comparator );
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
		if ( original == null ) {
			return null;
		}
		if ( !Hibernate.isInitialized( original ) ) {
			if ( ( (PersistentCollection) original ).hasQueuedOperations() ) {
				final AbstractPersistentCollection pc = (AbstractPersistentCollection) original;
				pc.replaceQueuedOperationValues( getCollectionPersister(), copyCache );
			}
			return target;
		}

		// for a null target, or a target which is the same as the original, we
		// need to put the merged elements in a new collection
		Object result = target == null || target == original ? instantiateResult( original ) : target;

		//for arrays, replaceElements() may return a different reference, since
		//the array length might not match
		result = replaceElements( original, result, owner, copyCache, session );

		if ( original == target ) {
			// get the elements back into the target making sure to handle dirty flag
			boolean wasClean = PersistentCollection.class.isInstance( target ) && !( ( PersistentCollection ) target ).isDirty();
			//TODO: this is a little inefficient, don't need to do a whole
			//      deep replaceElements() call
			replaceElements( result, target, owner, copyCache, session );
			if ( wasClean ) {
				( ( PersistentCollection ) target ).clearDirty();
			}
			result = target;
		}

		return result;
	}

	@Override
	public Object replace(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map copyCache,
			ForeignKeyDirection foreignKeyDirection) throws HibernateException {
		if ( ForeignKeyDirection.TO_PARENT == foreignKeyDirection ) {
			return replace( original, target, session, owner, copyCache );
		}
		else {
			return target;
		}
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
		return getCollectionPersister().getElementDescriptor().getOrmType();
	}

	@Override
	public Object indexOf(Object collection, Object element) {
		throw new UnsupportedOperationException( "generic collections don't have indexes" );
	}

	@Override
	public boolean contains(Object collection, Object childObject) {
		// we do not have to worry about queued additions to uninitialized
		// collections, since they can only occur for inverse collections!
		Iterator elems = getElementsIterator( collection );
		while ( elems.hasNext() ) {
			Object element = elems.next();
			// worrying about proxies is perhaps a little bit of overkill here...
			if ( element instanceof HibernateProxy ) {
				LazyInitializer li = ( (HibernateProxy) element ).getHibernateLazyInitializer();
				if ( !li.isUninitialized() ) {
					element = li.getImplementation();
				}
			}
			if ( element == childObject ) {
				return true;
			}
		}
		return false;
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

	/**
	 * Replace the elements of a collection with the elements of another collection.
	 *
	 * @param original The 'source' of the replacement elements (where we copy from)
	 * @param target The target of the replacement elements (where we copy to)
	 * @param owner The owner of the collection being merged
	 * @param copyCache The map of elements already replaced.
	 * @param session The session from which the merge event originated.
	 * @return The merged collection.
	 */
	protected Object replaceElements(
			Object original,
			Object target,
			Object owner,
			Map copyCache,
			SharedSessionContractImplementor session) {
		// TODO: does not work for EntityMode.DOM4J yet!
		java.util.Collection result = ( java.util.Collection ) target;
		result.clear();

		// copy elements into newly empty target collection
		Type elemType = getElementType( );
		Iterator iter = ( (java.util.Collection) original ).iterator();
		while ( iter.hasNext() ) {
			result.add( elemType.replace( iter.next(), null, session, owner, copyCache ) );
		}

		// if the original is a PersistentCollection, and that original
		// was not flagged as dirty, then reset the target's dirty flag
		// here afterQuery the copy operation.
		// </p>
		// One thing to be careful of here is a "bare" original collection
		// in which case we should never ever ever reset the dirty flag
		// on the target because we simply do not know...
		if ( original instanceof PersistentCollection ) {
			if ( result instanceof PersistentCollection ) {
				final PersistentCollection originalPersistentCollection = (PersistentCollection) original;
				final PersistentCollection resultPersistentCollection = (PersistentCollection) result;

				preserveSnapshot( originalPersistentCollection, resultPersistentCollection, elemType, owner, copyCache, session );

				if ( ! originalPersistentCollection.isDirty() ) {
					resultPersistentCollection.clearDirty();
				}
			}
		}

		return result;
	}

	/**
	 * Instantiate a new "underlying" collection exhibiting the same capacity
	 * charactersitcs and the passed "original".
	 *
	 * @param original The original collection.
	 * @return The newly instantiated collection.
	 */
	protected Object instantiateResult(Object original) {
		// by default just use an unanticipated capacity since we don't
		// know how to extract the capacity to use from original here...
		return instantiate( -1 );
	}

	private void preserveSnapshot(
			PersistentCollection original,
			PersistentCollection result,
			Type elemType,
			Object owner,
			Map copyCache,
			SharedSessionContractImplementor session) {
		final Serializable originalSnapshot = original.getStoredSnapshot();
		final Serializable resultSnapshot = result.getStoredSnapshot();
		Serializable targetSnapshot;

		if ( originalSnapshot instanceof List ) {
			targetSnapshot = new ArrayList(
					( (List) originalSnapshot ).size() );
			for ( Object obj : (List) originalSnapshot ) {
				( (List) targetSnapshot ).add( elemType.replace( obj, null, session, owner, copyCache ) );
			}
		}
		else if ( originalSnapshot instanceof Map ) {
			if ( originalSnapshot instanceof SortedMap ) {
				targetSnapshot = new TreeMap( ( (SortedMap) originalSnapshot ).comparator() );
			}
			else {
				targetSnapshot = new HashMap(
						CollectionHelper.determineProperSizing( ( (Map) originalSnapshot ).size() ),
						CollectionHelper.LOAD_FACTOR
				);
			}

			for ( Map.Entry<Object, Object> entry : ( (Map<Object, Object>) originalSnapshot ).entrySet() ) {
				Object key = entry.getKey();
				Object value = entry.getValue();
				Object resultSnapshotValue = ( resultSnapshot == null )
						? null
						: ( (Map<Object, Object>) resultSnapshot ).get( key );

				Object newValue = elemType.replace( value, resultSnapshotValue, session, owner, copyCache );

				if ( key == value ) {
					( (Map) targetSnapshot ).put( newValue, newValue );

				}
				else {
					( (Map) targetSnapshot ).put( key, newValue );
				}
			}
		}
		else if ( originalSnapshot instanceof Object[] ) {
			Object[] arr = (Object[]) originalSnapshot;
			for ( int i = 0; i < arr.length; i++ ) {
				arr[i] = elemType.replace( arr[i], null, session, owner, copyCache );
			}
			targetSnapshot = originalSnapshot;

		}
		else {
			// retain the same snapshot
			targetSnapshot = resultSnapshot;
		}

		final CollectionEntry ce = session.getPersistenceContext().getCollectionEntry( result );
		if ( ce != null ) {
			ce.resetStoredSnapshot( result, targetSnapshot );
		}
	}
}
