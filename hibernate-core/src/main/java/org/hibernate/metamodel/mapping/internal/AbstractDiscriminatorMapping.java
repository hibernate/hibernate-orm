/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.DiscriminatorType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.metamodel.RepresentationMode.MAP;

/**
 * @implNote `discriminatorType` represents the mapping to Class, whereas `discriminatorType.getUnderlyingType()`
 * represents the "raw" JDBC mapping (String, Integer, etc)
 *
 * @author Steve Ebersole
 */
public abstract class AbstractDiscriminatorMapping implements EntityDiscriminatorMapping {
	private final NavigableRole role;
	private final JdbcMapping jdbcMapping;

	private final EntityPersister entityDescriptor;
	private final DiscriminatorType<?> discriminatorType;
	private final SessionFactoryImplementor sessionFactory;

	private final DomainResultConverter<?> domainResultConverter;

	public AbstractDiscriminatorMapping(
			JdbcMapping jdbcMapping,
			EntityPersister entityDescriptor,
			DiscriminatorType<?> discriminatorType,
			MappingModelCreationProcess creationProcess) {
		this.jdbcMapping = jdbcMapping;

		this.entityDescriptor = entityDescriptor;
		this.discriminatorType = discriminatorType;

		role = entityDescriptor.getNavigableRole().append( EntityDiscriminatorMapping.ROLE_NAME );
		sessionFactory = creationProcess.getCreationContext().getSessionFactory();

		domainResultConverter = DomainResultConverter.create(
				entityDescriptor,
				this::getConcreteEntityNameForDiscriminatorValue,
				discriminatorType.getUnderlyingType(),
				sessionFactory
		);
	}

	public EntityPersister getEntityDescriptor() {
		return entityDescriptor;
	}

	public BasicType<?> getUnderlyingJdbcMappingType() {
		return discriminatorType.getUnderlyingType();
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EntityDiscriminatorMapping

	@Override
	public NavigableRole getNavigableRole() {
		return role;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public String getConcreteEntityNameForDiscriminatorValue(Object value) {
		return getEntityDescriptor().getSubclassForDiscriminatorValue( value );
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return entityDescriptor;
	}

	@Override
	public MappingType getMappedType() {
		return getJdbcMapping();
	}

	@Override
	public JavaType<?> getJavaType() {
		return getJdbcMapping().getJavaTypeDescriptor();
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlSelection sqlSelection = resolveSqlSelection(
				navigablePath,
				getUnderlyingJdbcMappingType(),
				tableGroup,
				null,
				creationState.getSqlAstCreationState()
		);

		//noinspection unchecked
		return new BasicResult(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				domainResultConverter.getDomainJavaType(),
				domainResultConverter,
				navigablePath
		);
	}

	private SqlSelection resolveSqlSelection(
			NavigablePath navigablePath,
			JdbcMapping jdbcMappingToUse,
			TableGroup tableGroup,
			FetchParent fetchParent,
			SqlAstCreationState creationState) {
		final SqlExpressionResolver expressionResolver = creationState.getSqlExpressionResolver();
		return expressionResolver.resolveSqlSelection(
				resolveSqlExpression( navigablePath, jdbcMappingToUse, tableGroup, creationState ),
				jdbcMappingToUse.getJavaTypeDescriptor(),
				fetchParent,
				creationState.getCreationContext().getSessionFactory().getTypeConfiguration()
		);
	}

	@Override
	public BasicFetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup(
				fetchParent.getNavigablePath()
		);

		assert tableGroup != null;

		final SqlSelection sqlSelection = resolveSqlSelection(
				fetchablePath,
				getUnderlyingJdbcMappingType(),
				tableGroup,
				fetchParent,
				creationState.getSqlAstCreationState()
		);

		return new BasicFetch<>(
				sqlSelection.getValuesArrayPosition(),
				fetchParent,
				fetchablePath,
				this,
				null,
				fetchTiming,
				creationState
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		resolveSqlSelection( navigablePath, getUnderlyingJdbcMappingType(), tableGroup, null, creationState.getSqlAstCreationState() );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		selectionConsumer.accept(
				resolveSqlSelection( navigablePath, getUnderlyingJdbcMappingType(), tableGroup, null, creationState.getSqlAstCreationState() ),
				getJdbcMapping()
		);
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, convertToRelational( value ), getJdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, getJdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		valueConsumer.consume( convertToRelational( domainValue ), this );
	}

	private Object convertToRelational(Object domainValue) {
		if ( domainResultConverter != null ) {
			return domainResultConverter.toRelationalValue( domainValue );
		}
		return domainValue;
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return convertToRelational( value );
	}

	@Override
	public int forEachJdbcValue(Object value, Clause clause, int offset, JdbcValuesConsumer valuesConsumer, SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, convertToRelational( value ), jdbcMapping );
		return 1;
	}

	@Override
	public int forEachSelectable(SelectableConsumer consumer) {
		return EntityDiscriminatorMapping.super.forEachSelectable( consumer );
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		return EntityDiscriminatorMapping.super.forEachSelectable( offset, consumer );
	}

	@Override
	public int forEachJdbcType(IndexedConsumer<JdbcMapping> action) {
		return EntityDiscriminatorMapping.super.forEachJdbcType( action );
	}



	/**
	 * Used to convert the underlying discriminator value into a Class (or String for entity-name)
	 * reference for the entity type
	 */
	protected static class DomainResultConverter<R> implements BasicValueConverter<Object,R> {
		/**
		 * Given a "raw" discriminator value, determines the corresponding concrete entity name
		 */
		private final Function<R,String> subtypeResolver;

		/**
		 * Given a concrete entity name, apply the conversion to determine the "domain result" value
		 *
		 * @apiNote This is only used when building a {@link DomainResult}
		 */
		private final Function<String, Object> entityNameHandler;

		/**
		 * Given a "domain form", apply the conversion to determine the corresponding relational value
		 */
		private final Function<Object, R> toRelationalConverter;

		private final JavaType<Object> domainJtd;
		private final JavaType<R> relationalJtd;

		public DomainResultConverter(
				Function<R,String> subtypeResolver,
				Function<String,Object> entityNameHandler,
				Function<Object,R> toRelationalConverter,
				JavaType<Object> domainJtd,
				JavaType<R> relationalJtd) {
			this.subtypeResolver = subtypeResolver;
			this.entityNameHandler = entityNameHandler;
			this.toRelationalConverter = toRelationalConverter;
			this.domainJtd = domainJtd;
			this.relationalJtd = relationalJtd;
		}

		private static <R> DomainResultConverter<R> create(
				EntityPersister entityDescriptor,
				Function<R,String> subtypeResolver,
				BasicType underlyingDiscriminatorType,
				final SessionFactoryImplementor sessionFactory) {
			final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();
			final JavaTypeRegistry jtdRegistry = typeConfiguration.getJavaTypeRegistry();

			final JavaType<Object> domainJtd;
			final Function<String,Object> entityNameHandler;
			final Function<Object,Object> toRelationalConverter;

			if ( entityDescriptor.getRepresentationStrategy().getMode() == MAP ) {
				// todo (6.0) : account for explicit entity-name which should also return String
				domainJtd = jtdRegistry.getDescriptor( String.class );
				entityNameHandler = (entityName) -> entityName;
				toRelationalConverter = (domainValue) -> {
					if ( domainValue instanceof Class ) {
						throw new IllegalArgumentException( "Illegal attempt to specify Class for discriminator for dynamic entity" );
					}
					if ( domainValue instanceof String ) {
						final String stringValue = (String) domainValue;
						// could be either an entity name or "underlying type" (e.g. mapped to VARCHAR)
						// 		- first we check as an entity name since that's a discrete set;
						//		- handling as an "underlying type" value is handled in "otherwise"
						if ( entityDescriptor.isSubclassEntityName( stringValue ) ) {
							return entityDescriptor.getDiscriminatorValue();
						}
					}

					// otherwise we assume its an instance of the underlying type
					assert underlyingDiscriminatorType.getJavaTypeDescriptor().getJavaTypeClass().isInstance( domainJtd );
					return domainJtd;
				};
			}
			else {
				final ClassLoaderService cls = sessionFactory.getServiceRegistry().getService( ClassLoaderService.class );
				domainJtd = jtdRegistry.getDescriptor( Class.class );
				entityNameHandler = cls::classForName;
				toRelationalConverter = (domainValue) -> {
					if ( domainValue instanceof Class ) {
						final Class<?> classValue = (Class<?>) domainValue;
						final EntityMappingType concreteEntityMapping = sessionFactory.getRuntimeMetamodels().getEntityMappingType( classValue );
						return concreteEntityMapping.getDiscriminatorValue();
					}
					if ( domainValue instanceof String ) {
						final String stringValue = (String) domainValue;
						// could be either an entity name or "underlying type" (e.g. mapped to VARCHAR)
						// 		- first we check as an entity name since that's a discrete set;
						//		- handling as an "underlying type" value is handled in "otherwise"
						if ( entityDescriptor.isSubclassEntityName( stringValue ) ) {
							return entityDescriptor.getDiscriminatorValue();
						}
					}

					// otherwise we assume its an instance of the underlying type
					assert underlyingDiscriminatorType.getJavaTypeDescriptor().getJavaTypeClass().isInstance( domainJtd );
					return domainJtd;
				};
			}

			return new DomainResultConverter(
					subtypeResolver,
					entityNameHandler,
					toRelationalConverter,
					domainJtd,
					underlyingDiscriminatorType.getJavaTypeDescriptor()
			);
		}

		@Override
		public Object toDomainValue(R relationalForm) {
			final String entityName = subtypeResolver.apply( relationalForm );
			return entityNameHandler.apply( entityName );
		}

		@Override
		public R toRelationalValue(Object domainForm) {
			// the domainForm could be any of Class (entity type), String (entity-name) or
			// underlying type (String, Integer, ..)

			return toRelationalConverter.apply( domainForm );
		}

		@Override
		public JavaType<Object> getDomainJavaType() {
			return domainJtd;
		}

		@Override
		public JavaType<R> getRelationalJavaType() {
			return relationalJtd;
		}
	}

}
