/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.entity;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.type.AbstractType;
import org.hibernate.type.Type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Loads an entity instance using outerjoin fetching to fetch associated entities.
 * <br>
 * The <tt>EntityPersister</tt> must implement <tt>Loadable</tt>. For other entities,
 * create a customized subclass of <tt>Loader</tt>.
 *
 * @author Gavin King
 */
public class EntityLoader extends AbstractEntityLoader {

	private final boolean batchLoader;
	private final int[][] compositeKeyManyToOneTargetIndices;

	public EntityLoader(
			OuterJoinLoadable persister,
			LockMode lockMode,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		this( persister, 1, lockMode, factory, loadQueryInfluencers );
	}

	public EntityLoader(
			OuterJoinLoadable persister,
			LockOptions lockOptions,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		this( persister, 1, lockOptions, factory, loadQueryInfluencers );
	}

	public EntityLoader(
			OuterJoinLoadable persister,
			int batchSize,
			LockMode lockMode,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		this(
				persister,
				persister.getIdentifierColumnNames(),
				persister.getIdentifierType(),
				batchSize,
				lockMode,
				factory,
				loadQueryInfluencers
			);
	}

	public EntityLoader(
			OuterJoinLoadable persister,
			int batchSize,
			LockOptions lockOptions,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		this(
				persister,
				persister.getIdentifierColumnNames(),
				persister.getIdentifierType(),
				batchSize,
				lockOptions,
				factory,
				loadQueryInfluencers
			);
	}

	public EntityLoader(
			OuterJoinLoadable persister,
			String[] uniqueKey,
			Type uniqueKeyType,
			int batchSize,
			LockMode lockMode,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		super( persister, uniqueKeyType, factory, loadQueryInfluencers );

		EntityJoinWalker walker = new EntityJoinWalker(
				persister,
				uniqueKey,
				batchSize,
				lockMode,
				factory,
				loadQueryInfluencers
		);
		initFromWalker( walker );
		compositeKeyManyToOneTargetIndices = walker.getCompositeKeyManyToOneTargetIndices();
		postInstantiate();

		batchLoader = batchSize > 1;

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Static select for entity %s [%s]: %s", entityName, lockMode, getSQLString() );
		}
	}

	public EntityLoader(
			OuterJoinLoadable persister,
			String[] uniqueKey,
			Type uniqueKeyType,
			int batchSize,
			LockOptions lockOptions,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		super( persister, uniqueKeyType, factory, loadQueryInfluencers );

		EntityJoinWalker walker = new EntityJoinWalker(
				persister,
				uniqueKey,
				batchSize,
				lockOptions,
				factory,
				loadQueryInfluencers
		);
		initFromWalker( walker );
		compositeKeyManyToOneTargetIndices = walker.getCompositeKeyManyToOneTargetIndices();
		postInstantiate();

		batchLoader = batchSize > 1;

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Static select for entity %s [%s:%s]: %s",
					entityName,
					lockOptions.getLockMode(),
					lockOptions.getTimeOut(),
					getSQLString() );
		}
	}

	public EntityLoader(
			OuterJoinLoadable persister,
			boolean[] valueNullness,
			int batchSize,
			LockOptions lockOptions,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		super( persister, new NaturalIdType( persister, valueNullness ), factory, loadQueryInfluencers );

		EntityJoinWalker walker = new EntityJoinWalker(
				persister,
				naturalIdColumns( valueNullness ),
				batchSize,
				lockOptions,
				factory,
				loadQueryInfluencers
		) {
			@Override
			protected StringBuilder whereString(String alias, String[] columnNames, int batchSize) {
				StringBuilder sql = super.whereString(alias, columnNames, batchSize);
				for (String nullCol : naturalIdColumns( ArrayHelper.negate( valueNullness ) ) ) {
					sql.append(" and ").append( getAlias() ).append('.').append(nullCol).append(" is null");
				}
				return sql;
			}
		};
		initFromWalker( walker );
		compositeKeyManyToOneTargetIndices = walker.getCompositeKeyManyToOneTargetIndices();
		postInstantiate();

		batchLoader = batchSize > 1;

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Static select for entity %s [%s:%s]: %s",
					entityName,
					lockOptions.getLockMode(),
					lockOptions.getTimeOut(),
					getSQLString() );
		}
	}

	private String[] naturalIdColumns(boolean[] valueNullness) {
		int i = 0;
		List<String> columns = new ArrayList<>();
		for ( int p : persister.getNaturalIdentifierProperties() ) {
			if ( !valueNullness[i++] ) {
				columns.addAll( Arrays.asList( persister.getPropertyColumnNames(p) ) );
			}
		}
		return columns.toArray(ArrayHelper.EMPTY_STRING_ARRAY);
	}

	public Object loadByUniqueKey(SharedSessionContractImplementor session, Object key) {
		return loadByUniqueKey( session, key, null );
	}

	public Object loadByUniqueKey(SharedSessionContractImplementor session, Object key, Boolean readOnly) {
		return load( session, key, null, null, LockOptions.NONE, readOnly );
	}

	@Override
	protected boolean isSingleRowLoader() {
		return !batchLoader;
	}

	@Override
	public int[][] getCompositeKeyManyToOneTargetIndices() {
		return compositeKeyManyToOneTargetIndices;
	}

	static class NaturalIdType extends AbstractType {
		private OuterJoinLoadable persister;
		private boolean[] valueNullness;

		NaturalIdType(OuterJoinLoadable persister, boolean[] valueNullness) {
			this.persister = persister;
			this.valueNullness = valueNullness;
		}

		@Override
		public int getColumnSpan(Mapping mapping) throws MappingException {
			int span = 0;
			int i = 0;
			for ( int p : persister.getNaturalIdentifierProperties() ) {
				if ( !valueNullness[i++] ) {
					span += persister.getPropertyColumnNames(p).length;
				}
			}
			return span;
		}

		@Override
		public int[] sqlTypes(Mapping mapping) throws MappingException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Size[] dictatedSizes(Mapping mapping) throws MappingException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Size[] defaultSizes(Mapping mapping) throws MappingException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Class getReturnedClass() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isDirty(Object oldState, Object currentState, boolean[] checkable, SharedSessionContractImplementor session)
				throws HibernateException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
				throws HibernateException, SQLException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object nullSafeGet(ResultSet rs, String name, SharedSessionContractImplementor session, Object owner)
				throws HibernateException, SQLException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SharedSessionContractImplementor session)
				throws HibernateException, SQLException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
				throws HibernateException, SQLException {
			Object[] keys = (Object[]) value;
			int i = 0;
			for ( int p : persister.getNaturalIdentifierProperties() ) {
				if ( !valueNullness[i] ) {
					persister.getPropertyTypes()[p].nullSafeSet( st, keys[i], index++, session );
				}
				i++;
			}
		}

		@Override
		public String toLoggableString(Object value, SessionFactoryImplementor factory) {
			return "natural id";
		}

		@Override
		public String getName() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object deepCopy(Object value, SessionFactoryImplementor factory) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isMutable() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object resolve(Object value, SharedSessionContractImplementor session, Object owner, Boolean overridingEager) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner, Map copyCache) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean[] toColumnNullness(Object value, Mapping mapping) {
			throw new UnsupportedOperationException();
		}
	}
}
