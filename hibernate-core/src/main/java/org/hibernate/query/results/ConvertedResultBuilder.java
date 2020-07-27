/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import javax.persistence.AttributeConverter;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.converter.ConvertedValueExtractor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A ResultBuilder for explicitly converted scalar results
 *
 * @author Steve Ebersole
 */
public class ConvertedResultBuilder implements ScalarResultBuilder {

	public static <C> ResultBuilder from(
			String columnAlias,
			Class<C> relationalJavaType,
			AttributeConverter<?, C> converter,
			SessionFactoryImplementor sessionFactory) {
		final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();
		final JavaTypeDescriptorRegistry jtdRegistry = typeConfiguration.getJavaTypeDescriptorRegistry();
		final JavaTypeDescriptor<C> relationJtd = jtdRegistry.getDescriptor( relationalJavaType );

		return new ConvertedResultBuilder( columnAlias, relationJtd, converter );
	}

	public static <C> ResultBuilder from(
			String columnAlias,
			Class<C> relationalJavaType,
			Class<? extends AttributeConverter<?, C>> converterJavaType,
			SessionFactoryImplementor sessionFactory) {
		final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();
		final JavaTypeDescriptorRegistry jtdRegistry = typeConfiguration.getJavaTypeDescriptorRegistry();
		final JavaTypeDescriptor<C> relationJtd = jtdRegistry.getDescriptor( relationalJavaType );

		final ManagedBeanRegistry beans = sessionFactory.getServiceRegistry().getService( ManagedBeanRegistry.class );
		final ManagedBean<? extends AttributeConverter<?, C>> bean = beans.getBean( converterJavaType );
		final AttributeConverter<?, C> converter = bean.getBeanInstance();

		return new ConvertedResultBuilder( columnAlias, relationJtd, converter );
	}

	private final String columnAlias;
	private final JavaTypeDescriptor<?> relationJtd;
	private final AttributeConverter<?,?> converter;

	public ConvertedResultBuilder(
			String columnAlias,
			JavaTypeDescriptor<?> relationJtd,
			AttributeConverter<?, ?> converter) {
		assert columnAlias != null;
		this.columnAlias = columnAlias;

		assert relationJtd != null;
		this.relationJtd = relationJtd;

		assert converter != null;
		this.converter = converter;

	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public DomainResult<?> buildReturn(
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, LegacyFetchBuilder> legacyFetchResolver,
			Consumer<SqlSelection> sqlSelectionConsumer,
			SessionFactoryImplementor sessionFactory) {
		final int jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( columnAlias );
		final int valuesArrayPosition = jdbcPosition - 1;

		final SqlTypeDescriptor std = jdbcResultsMetadata.resolveSqlTypeDescriptor( jdbcPosition );

		final ConverterMapping<Object> converterMapping = new ConverterMapping(
				relationJtd,
				std,
				new ConvertedValueExtractor( std.getExtractor( relationJtd ), converter )
		);

		final SqlSelectionImpl sqlSelection = new SqlSelectionImpl( valuesArrayPosition, converterMapping );
		sqlSelectionConsumer.accept( sqlSelection );

		return new BasicResult( valuesArrayPosition, columnAlias, converterMapping.getJavaTypeDescriptor() );
	}


	private static class ConverterMapping<T> implements MappingModelExpressable<T>, JdbcMapping {
		private final JavaTypeDescriptor<T> jtd;
		private final SqlTypeDescriptor std;

		private final ValueExtractor<T> extractor;

		public ConverterMapping(
				JavaTypeDescriptor<T> jtd,
				SqlTypeDescriptor std,
				ValueExtractor<T> extractor) {
			this.jtd = jtd;
			this.std = std;
			this.extractor = extractor;
		}

		@Override
		public int getJdbcTypeCount(TypeConfiguration typeConfiguration) {
			return 1;
		}

		@Override
		public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
			return jtd;
		}

		@Override
		public SqlTypeDescriptor getSqlTypeDescriptor() {
			return std;
		}

		@Override
		public ValueExtractor<T> getJdbcValueExtractor() {
			return extractor;
		}

		@Override
		public ValueBinder<?> getJdbcValueBinder() {
			// this will never get used for binding values
			throw new UnsupportedOperationException();
		}

		@Override
		public List<JdbcMapping> getJdbcMappings(TypeConfiguration typeConfiguration) {
			return Collections.singletonList( this );
		}

		@Override
		public void visitJdbcTypes(
				Consumer<JdbcMapping> action,
				Clause clause,
				TypeConfiguration typeConfiguration) {
			action.accept( this );
		}
	}

	@SuppressWarnings("rawtypes")
	private static class SqlSelectionImpl implements SqlSelection {
		private final int valuesArrayPosition;
		private final ConverterMapping converterMapping;

		public SqlSelectionImpl(
				int valuesArrayPosition,
				ConverterMapping converterMapping) {
			this.valuesArrayPosition = valuesArrayPosition;
			this.converterMapping = converterMapping;
		}

		@Override
		public int getValuesArrayPosition() {
			return valuesArrayPosition;
		}

		@Override
		public ValueExtractor getJdbcValueExtractor() {
			return converterMapping.getJdbcValueExtractor();
		}

		@Override
		public MappingModelExpressable getExpressionType() {
			return converterMapping;
		}

		@Override
		public void accept(SqlAstWalker sqlAstWalker) {
			throw new UnsupportedOperationException();
		}
	}
}
