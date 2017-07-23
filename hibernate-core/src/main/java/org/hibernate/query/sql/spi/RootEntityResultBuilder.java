/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.sql.AttributeResultRegistration;
import org.hibernate.query.sql.EntityResultRegistration;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.tree.internal.select.QueryResultEntityImpl;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;
import org.hibernate.sql.ast.tree.spi.select.EntityIdentifierReference;
import org.hibernate.sql.ast.tree.spi.select.Fetch;
import org.hibernate.sql.ast.tree.spi.select.QueryResultEntity;
import org.hibernate.sql.exec.results.internal.EntityReturnInitializerImpl;
import org.hibernate.sql.exec.results.internal.QueryResultAssemblerEntity;
import org.hibernate.sql.exec.results.spi.Initializer;
import org.hibernate.sql.exec.results.spi.InitializerCollector;
import org.hibernate.sql.exec.results.spi.InitializerParent;
import org.hibernate.sql.exec.results.spi.QueryResultAssembler;
import org.hibernate.sql.exec.results.spi.SqlSelectionGroup;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class RootEntityResultBuilder
		implements ReturnableResultNodeImplementor, NativeQuery.RootReturn, EntityResultRegistration {
	private final String tableAlias;
	private final String entityName;
	private LockMode lockMode = LockMode.READ;

	private List<String> idColumnAliases;
	private String discriminatorColumnAlias;

	private Map<String, AttributeResultBuilder> propertyMappings;

	public RootEntityResultBuilder(String tableAlias, String entityName) {
		this.tableAlias = tableAlias;
		this.entityName = entityName;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// NativeQuery.RootReturn

	public NativeQuery.RootReturn setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
		return this;
	}

	@Override
	public NativeQuery.RootReturn addIdColumnAliases(String... aliases) {
		if ( aliases != null ) {
			if ( aliases.length == 1 ) {
				idColumnAliases.add( aliases[0] );
			}
			else {
				idColumnAliases.addAll( Arrays.asList( aliases ) );
			}
		}

		return this;
	}

	public NativeQuery.RootReturn setDiscriminatorAlias(String alias) {
		this.discriminatorColumnAlias = alias;
		return this;
	}

	public NativeQuery.RootReturn addProperty(String propertyName, String columnAlias) {
		addProperty( propertyName ).addColumnAlias( columnAlias );
		return this;
	}

	public NativeQuery.ReturnProperty addProperty(final String propertyName) {
		if ( propertyMappings == null ) {
			propertyMappings = new HashMap<>();
		}

		return new NativeQuery.ReturnProperty() {
			public NativeQuery.ReturnProperty addColumnAlias(String columnAlias) {
				final AttributeResultBuilder registration = propertyMappings.computeIfAbsent(
						propertyName,
						AttributeResultBuilder::new
				);
				registration.addColumnAlias( columnAlias );
				return this;
			}
		};
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EntityResultRegistration

	@Override
	public String getEntityName() {
		return entityName;
	}

	@Override
	public String getTableAlias() {
		return tableAlias;
	}

	@Override
	public LockMode getLockMode() {
		return lockMode;
	}

	@Override
	public List<String> getIdColumnAliases() {
		return idColumnAliases;
	}

	@Override
	public String getDiscriminatorColumnAlias() {
		return discriminatorColumnAlias;
	}

	@Override
	public List<AttributeResultRegistration> getAttributeResultRegistrations() {
		return null;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// NativeQueryReturnBuilder

	@Override
	public QueryResultEntity buildReturn(NodeResolutionContext resolutionContext) {

		return new QueryResultEntity() {
			final EntityReturnInitializerImpl initializer = new EntityReturnInitializerImpl(
					this,
					resolveSqlSelectionMap( resolutionContext ),
					false
			);
			final QueryResultAssemblerEntity assembler = new QueryResultAssemblerEntity( this );

			@Override
			public EntityDescriptor getEntityDescriptor() {
				return resolutionContext.getSessionFactory().getTypeConfiguration().findEntityDescriptor( getEntityName() );
			}

			@Override
			public EntityIdentifierReference getIdentifierReference() {
				throw new NotYetImplementedException(  );
			}

			@Override
			public NavigableContainerReference getNavigableContainerReference() {
				// none for a root
				return null;
			}

			@Override
			public InitializerParent getInitializerParentForFetchInitializers() {
				return null;
			}

			@Override
			public void addFetch(Fetch fetch) {

			}

			@Override
			public List<Fetch> getFetches() {
				return null;
			}

			@Override
			public String getSelectedExpressionDescription() {
				return null;
			}

			@Override
			public String getResultVariable() {
				return null;
			}

			@Override
			public JavaTypeDescriptor getJavaTypeDescriptor() {
				return null;
			}

			@Override
			public QueryResultAssembler getResultAssembler() {
				return null;
			}

			@Override
			public void registerInitializers(InitializerCollector collector) {

			}

			@Override
			public Initializer getInitializer() {
				return null;
			}
		};
		throw new NotYetImplementedException(  );
//		return new QueryResultEntityImpl( ... );
	}

	private Map<PersistentAttribute, SqlSelectionGroup> resolveSqlSelectionMap(NodeResolutionContext resolutionContext) {
		throw new NotYetImplementedException(  );
	}

}
