/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi.bind;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.spi.Binding;
import org.hibernate.engine.jdbc.mutation.spi.BindingGroup;
import org.hibernate.engine.jdbc.mutation.spi.JdbcValueDescriptorAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.PreparableMutationOperation;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;

/// Used to track JDBC value bindings (generally parameters) used in mutation operations.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public class JdbcValueBindings {
	private final TableDescriptor tableDescriptor;
	private final JdbcValueDescriptorAccess jdbcValueDescriptorAccess;
	private final MutationBindTemplate bindTemplate;
	private final Object[] valuesBySlot;
	private final boolean[] boundSlots;
	private BindingGroup bindingGroup;

	public JdbcValueBindings(TableDescriptor tableDescriptor, JdbcValueDescriptorAccess jdbcValueDescriptorAccess) {
		this.tableDescriptor = tableDescriptor;
		this.jdbcValueDescriptorAccess = jdbcValueDescriptorAccess;
		this.bindTemplate = jdbcValueDescriptorAccess instanceof PreparableMutationOperation preparable
				? MutationBindTemplate.forOperation( preparable )
				: null;
		if ( bindTemplate == null ) {
			this.valuesBySlot = null;
			this.boundSlots = null;
			this.bindingGroup = new BindingGroup( tableDescriptor.name() );
		}
		else {
			this.valuesBySlot = new Object[bindTemplate.slots().length];
			this.boundSlots = new boolean[bindTemplate.slots().length];
		}
	}

	@SuppressWarnings("unchecked")
	public void beforeStatement(PreparedStatement preparedStatement, SharedSessionContractImplementor session) {
		if ( bindTemplate == null ) {
			bindingGroup.forEachBinding( (binding) -> {
				try {
					binding.getValueBinder().bind(
							preparedStatement,
							resolveValue( binding.getValue() ),
							binding.getPosition(),
							session
					);
				}
				catch (SQLException e) {
					throw session.getJdbcServices().getSqlExceptionHelper().convert(
							e,
							String.format(
									Locale.ROOT,
									"Unable to bind parameter #%s - %s",
									binding.getPosition(),
									binding.getValue()
							)
					);
				}
			} );
			return;
		}

		final BindSlot[] slots = bindTemplate.slots();
		for ( int i = 0; i < slots.length; i++ ) {
			if ( !boundSlots[i] ) {
				continue;
			}
			final BindSlot slot = slots[i];
			try {
				slot.jdbcMapping().getJdbcValueBinder().bind(
						preparedStatement,
						resolveValue( valuesBySlot[i] ),
						slot.jdbcPosition(),
						session
				);
			}
			catch (SQLException e) {
				throw session.getJdbcServices().getSqlExceptionHelper().convert(
						e,
						String.format(
								Locale.ROOT,
								"Unable to bind parameter #%s - %s",
								slot.jdbcPosition(),
								valuesBySlot[i]
						)
				);
			}
		}
	}

	public static Object resolveValue(Object value) {
		return value instanceof DelayedValueAccess handle ? handle.get() : value;
	}

	public void bindValue(Object columnValue, String columnName, ParameterUsage parameterUsage) {
		if ( bindTemplate != null ) {
			final BindSlot slot = bindTemplate.findSlot( columnName, parameterUsage );
			if ( slot == null ) {
				throw new HibernateException( "Unable to locate JdbcValueDescriptor for column `" + columnName + "`" );
			}
			if ( !boundSlots[slot.index()] ) {
				valuesBySlot[slot.index()] = columnValue;
				boundSlots[slot.index()] = true;
			}
			return;
		}

		final var jdbcValueDescriptor = jdbcValueDescriptorAccess.resolveValueDescriptor(
				tableDescriptor.name(),
				columnName,
				parameterUsage
		);
		if ( jdbcValueDescriptor == null ) {
			throw new HibernateException( "Unable to locate JdbcValueDescriptor for column `" + columnName + "`" );
		}
		bindingGroup.bindValue( columnName, columnValue, jdbcValueDescriptor );
	}

	public Object getBoundValue(String columnName, ParameterUsage usage) {
		if ( bindTemplate != null ) {
			final BindSlot slot = bindTemplate.findSlot( columnName, usage );
			return slot == null || !boundSlots[slot.index()] ? null : valuesBySlot[slot.index()];
		}
		final Binding binding = bindingGroup.findBinding( columnName, usage );
		return binding == null ? null : binding.getValue();
	}

	public boolean hasBinding(String columnName, ParameterUsage usage) {
		if ( bindTemplate != null ) {
			final BindSlot slot = bindTemplate.findSlot( columnName, usage );
			return slot != null && boundSlots[slot.index()];
		}
		return bindingGroup.findBinding( columnName, usage ) != null;
	}

	public BindingGroup getBindingGroup() {
		if ( bindTemplate != null ) {
			final BindingGroup compatibilityGroup = new BindingGroup( tableDescriptor.name() );
			final BindSlot[] slots = bindTemplate.slots();
			for ( int i = 0; i < slots.length; i++ ) {
				if ( boundSlots[i] ) {
					final BindSlot slot = slots[i];
					compatibilityGroup.bindValue( slot.columnName(), valuesBySlot[i], slot.valueDescriptor() );
				}
			}
			return compatibilityGroup;
		}
		return bindingGroup;
	}

	public void replaceValue(String columnName, ParameterUsage parameterUsage, Object newValue) {
		if ( bindTemplate != null ) {
			final BindSlot slot = bindTemplate.findSlot( columnName, parameterUsage );
			if ( slot == null || !boundSlots[slot.index()] ) {
				throw new IllegalArgumentException( String.format( Locale.ROOT,
						"Could not locate binding [%s : %s]",
						parameterUsage.toString(),
						columnName
				) );
			}
			valuesBySlot[slot.index()] = newValue;
			return;
		}
		final Binding binding = bindingGroup.getBinding( columnName, parameterUsage );
		binding.setValue( newValue );
	}

	public void clear() {
		if ( bindTemplate != null ) {
			Arrays.fill( valuesBySlot, null );
			Arrays.fill( boundSlots, false );
		}
		else {
			bindingGroup.clear();
		}
	}

	/// Form of [#bindValue(Object, String, ParameterUsage)] which is intended for use
	/// as a [org.hibernate.metamodel.mapping.ModelPart.JdbcValueConsumer] with [ParameterUsage#SET] semantics.
	///
	/// @see org.hibernate.metamodel.mapping.ModelPart.JdbcValueConsumer#consume(int, Object, SelectableMapping)
	public void bindAssignment(@SuppressWarnings("unused") int valueIndex, Object value, SelectableMapping jdbcValueMapping) {
		if ( jdbcValueMapping.isFormula() ) {
			// derived values should NEVER be part of the assignment
			return;
		}
		bindValue( value, jdbcValueMapping.getSelectionExpression(), ParameterUsage.SET );
	}

	/// Form of [#bindAssignment(int, Object, SelectableMapping)] which performs the binding
	/// only if the passed `jdbcValueMapping` is [insertable][SelectableMapping#isInsertable()].
	///
	/// @apiNote We define this as a separate method to avoid lambda creation.
	public void bindInsertAssignment(@SuppressWarnings("unused") int valueIndex, Object value, SelectableMapping jdbcValueMapping) {
		if ( !jdbcValueMapping.isFormula() && jdbcValueMapping.isInsertable() ) {
			bindValue( value, jdbcValueMapping.getSelectionExpression(), ParameterUsage.SET );
		}
	}

	/// Form of [#bindAssignment(int, Object, SelectableMapping)] which performs the binding
	/// only if the passed `jdbcValueMapping` is [updateable][SelectableMapping#isUpdateable()].
	///
	/// @apiNote We define this as a separate method to avoid lambda creation.
	public void bindUpdateAssignment(@SuppressWarnings("unused") int valueIndex, Object value, SelectableMapping jdbcValueMapping) {
		if ( !jdbcValueMapping.isFormula() && jdbcValueMapping.isUpdateable() ) {
			bindValue( value, jdbcValueMapping.getSelectionExpression(), ParameterUsage.SET );
		}
	}

	/// Form of [#bindValue(Object, String, ParameterUsage)] which is intended for use
	/// as a [org.hibernate.metamodel.mapping.ModelPart.JdbcValueConsumer] with [ParameterUsage#RESTRICT] semantics.
	///
	///
	/// @see org.hibernate.metamodel.mapping.ModelPart.JdbcValueConsumer#consume(int, Object, SelectableMapping)
	public void bindRestriction(@SuppressWarnings("unused") int valueIndex, Object value, SelectableMapping jdbcValueMapping) {
		if ( jdbcValueMapping.isFormula() ) {
			return;
		}
		bindValue( value, jdbcValueMapping.getSelectionExpression(), ParameterUsage.RESTRICT );
	}

	/// Form of [#bindRestriction(int, Object, SelectableMapping)] which performs the binding
	/// only if the passed `jdbcValueMapping` is [updateable][SelectableMapping#isUpdateable()].
	///
	/// @apiNote We define this as a separate method to avoid lambda creation.
	public void bindUpdateRestriction(@SuppressWarnings("unused") int valueIndex, Object value, SelectableMapping jdbcValueMapping) {
		if ( !jdbcValueMapping.isFormula() && jdbcValueMapping.isUpdateable() ) {
			bindValue( value, jdbcValueMapping.getSelectionExpression(), ParameterUsage.RESTRICT );
		}
	}
}
