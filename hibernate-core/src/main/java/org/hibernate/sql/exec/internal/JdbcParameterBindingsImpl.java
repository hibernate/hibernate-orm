/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.BindableType;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sql.internal.NativeQueryImpl;
import org.hibernate.query.sql.spi.ParameterOccurrence;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;

/**
 * Standard implementation of JdbcParameterBindings
 *
 * @author Steve Ebersole
 */
public class JdbcParameterBindingsImpl implements JdbcParameterBindings {
	private Map<JdbcParameter, JdbcParameterBinding> bindingMap;
	private Set<String> affectedTableName;

	public JdbcParameterBindingsImpl(int expectedParameterCount) {
		if ( expectedParameterCount > 0 ) {
			bindingMap = new IdentityHashMap<>( expectedParameterCount );
		}
	}

	public JdbcParameterBindingsImpl(
			QueryParameterBindings queryParameterBindings,
			List<ParameterOccurrence> parameterOccurrences,
			List<JdbcParameterBinder> jdbcParameterBinders,
			SessionFactoryImplementor factory) {
		if ( !parameterOccurrences.isEmpty() ) {
			bindingMap = new IdentityHashMap<>( parameterOccurrences.size() );

			final Dialect dialect = factory.getJdbcServices().getDialect();
			final boolean paddingEnabled = factory.getSessionFactoryOptions().inClauseParameterPaddingEnabled();
			final int inExprLimit = dialect.getParameterCountLimit();

			for ( ParameterOccurrence occurrence : parameterOccurrences ) {
				final QueryParameterImplementor<?> param = occurrence.getParameter();
				final QueryParameterBinding<?> binding = queryParameterBindings.getBinding( param );

				final JdbcMapping jdbcMapping;

				final BindableType<?> type = determineParamType( param, binding );
				if ( type == null ) {
					jdbcMapping = factory.getTypeConfiguration().getBasicTypeForJavaType( Object.class );
				}
				else if ( type instanceof BasicTypeReference ) {
					jdbcMapping = factory.getTypeConfiguration()
							.getBasicTypeRegistry()
							.resolve( ( (BasicTypeReference<?>) type ) );
				}
				else if ( type instanceof BasicValuedMapping ) {
					jdbcMapping = ( (BasicValuedMapping) type ).getJdbcMapping();
				}
				else {
					throw new IllegalArgumentException( "Could not resolve NativeQuery parameter type : `" + param + "`");
				}

				final BasicValueConverter valueConverter = jdbcMapping == null ? null : jdbcMapping.getValueConverter();

				if ( binding.isMultiValued() ) {
					final Collection<?> bindValues = binding.getBindValues();
					final int bindValueCount = bindValues.size();
					final int bindValueMaxCount = NativeQueryImpl.determineBindValueMaxCount(
							paddingEnabled,
							inExprLimit,
							bindValueCount
					);
					Object lastBindValue = null;
					if ( valueConverter != null ) {
						for ( Object bindValue : bindValues ) {
							final JdbcParameterImpl jdbcParameter = new JdbcParameterImpl( jdbcMapping );
							jdbcParameterBinders.add( jdbcParameter );
							lastBindValue = valueConverter.toRelationalValue( bindValue );
							addBinding( jdbcParameter, new JdbcParameterBindingImpl( jdbcMapping, lastBindValue ) );
						}
						if ( bindValueMaxCount != bindValueCount ) {
							for ( int i = bindValueCount; i < bindValueMaxCount; i++ ) {
								final JdbcParameterImpl jdbcParameter = new JdbcParameterImpl( jdbcMapping );
								jdbcParameterBinders.add( jdbcParameter );
								addBinding( jdbcParameter, new JdbcParameterBindingImpl( jdbcMapping, lastBindValue ) );
							}
						}
					}
					else {
						for ( Object bindValue : bindValues ) {
							final JdbcParameterImpl jdbcParameter = new JdbcParameterImpl( jdbcMapping );
							jdbcParameterBinders.add( jdbcParameter );
							addBinding( jdbcParameter, new JdbcParameterBindingImpl( jdbcMapping, bindValue ) );
							lastBindValue = bindValue;
						}
						if ( bindValueMaxCount != bindValueCount ) {
							for ( int i = bindValueCount; i < bindValueMaxCount; i++ ) {
								final JdbcParameterImpl jdbcParameter = new JdbcParameterImpl( jdbcMapping );
								jdbcParameterBinders.add( jdbcParameter );
								addBinding( jdbcParameter, new JdbcParameterBindingImpl( jdbcMapping, lastBindValue ) );
							}
						}
					}
				}
				else {
					final Object bindValue;
					if ( valueConverter != null && binding.getBindValue() != null ) {
						bindValue = valueConverter.toRelationalValue( binding.getBindValue() );
					}
					else {
						bindValue = binding.getBindValue();
					}

					final JdbcParameterImpl jdbcParameter = new JdbcParameterImpl( jdbcMapping );
					jdbcParameterBinders.add( jdbcParameter );
					addBinding(
							jdbcParameter,
							new JdbcParameterBindingImpl( jdbcMapping, bindValue )
					);
				}
			}
		}
	}

	private BindableType<?> determineParamType(QueryParameterImplementor<?> param, QueryParameterBinding<?> binding) {
		BindableType<?> type = binding.getBindType();
		if ( type == null ) {
			type = param.getHibernateType();
		}
		return type;
	}

	@Override
	public void addBinding(JdbcParameter parameter, JdbcParameterBinding binding) {
		if ( bindingMap == null ) {
			bindingMap = new IdentityHashMap<>();
		}

		bindingMap.put( parameter, binding );
	}

	@Override
	public Collection<JdbcParameterBinding> getBindings() {
		return bindingMap == null ? Collections.emptyList() : bindingMap.values();
	}

	@Override
	public JdbcParameterBinding getBinding(JdbcParameter parameter) {
		if ( bindingMap == null ) {
			return null;
		}
		return bindingMap.get( parameter );
	}

	@Override
	public void visitBindings(BiConsumer<JdbcParameter, JdbcParameterBinding> action) {
		if ( bindingMap == null ) {
			return;
		}
		for ( Map.Entry<JdbcParameter, JdbcParameterBinding> entry : bindingMap.entrySet() ) {
			action.accept( entry.getKey(), entry.getValue() );
		}
	}

	public void clear() {
		if ( bindingMap != null ) {
			bindingMap.clear();
		}
	}

	@Override
	public Set<String> getAffectedTableNames() {
		return affectedTableName;
	}

	@Override
	public void addAffectedTableName(String tableName) {
		if ( affectedTableName == null ) {
			affectedTableName = new HashSet<>( bindingMap.size() );
		}
		affectedTableName.add( tableName );
	}
}
