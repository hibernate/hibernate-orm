/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.Bindable;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.internal.BindingTypeHelper;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Access to all the externalized JDBC parameter bindings
 *
 * @apiNote "Externalized" because some JDBC parameter values are
 * intrinsically part of the parameter itself, and we do not need to
 * locate a JdbcParameterBinding.  E.g., consider a
 * {@link org.hibernate.sql.ast.tree.expression.LiteralAsParameter}
 * which encapsulates the literal value inside itself - to create the
 * binder and actually perform the binding is only dependent on the
 * LiteralParameter
 *
 * @author Steve Ebersole
 */
public interface JdbcParameterBindings {
	void addBinding(JdbcParameter parameter, JdbcParameterBinding binding);

	Collection<JdbcParameterBinding> getBindings();

	JdbcParameterBinding getBinding(JdbcParameter parameter);

	void visitBindings(BiConsumer<JdbcParameter, JdbcParameterBinding> action);

	JdbcParameterBindings NO_BINDINGS = new JdbcParameterBindings() {
		@Override
		public void addBinding(JdbcParameter parameter, JdbcParameterBinding binding) {
		}

		@Override
		public Collection<JdbcParameterBinding> getBindings() {
			return Collections.emptyList();
		}

		@Override
		public JdbcParameterBinding getBinding(JdbcParameter parameter) {
			return null;
		}

		@Override
		public void visitBindings(BiConsumer<JdbcParameter, JdbcParameterBinding> action) {
		}
	};

	default int registerParametersForEachJdbcValue(
			Object value,
			Bindable bindable,
			JdbcParametersList jdbcParameters,
			SharedSessionContractImplementor session) {
		return registerParametersForEachJdbcValue( value, 0, bindable, jdbcParameters, session );
	}

	default int registerParametersForEachJdbcValue(
			Object value,
			int offset,
			Bindable bindable,
			JdbcParametersList jdbcParameters,
			SharedSessionContractImplementor session) {
		final Object valueToBind;
		if ( bindable instanceof BasicValuedMapping ) {
			valueToBind = ( (BasicValuedMapping) bindable ).getJdbcMapping().getMappedJavaType().wrap( value, session );
		}
		else {
			valueToBind = value;
		}
		return bindable.forEachJdbcValue(
				valueToBind,
				offset,
				jdbcParameters,
				session.getFactory().getTypeConfiguration(),
				this::createAndAddBinding,
				session
		);
	}

	private void createAndAddBinding(
			int selectionIndex,
			JdbcParametersList params,
			TypeConfiguration typeConfiguration,
			Object jdbcValue,
			JdbcMapping type) {
		addBinding(
				params.get( selectionIndex ),
				new JdbcParameterBindingImpl(
						BindingTypeHelper.INSTANCE.resolveBindType( jdbcValue, type, typeConfiguration ),
						jdbcValue
				)
		);
	}

	default Set<String> getAffectedTableNames(){
		return null;
	}

	default void addAffectedTableName(String tableName){

	}
}
