/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.embedded;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.internal.domain.AbstractFetchParent;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.CompositeInitializer;
import org.hibernate.sql.results.spi.CompositeMappingNode;
import org.hibernate.sql.results.spi.CompositeResult;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Andrea Boriero
 */
public class CompositeForeignKeyResultImpl extends AbstractFetchParent implements CompositeResult {
	final private List<SqlSelection> sqlSelections;

	public CompositeForeignKeyResultImpl(
			EmbeddedValuedNavigable navigable,
			List<SqlSelection> sqlSelections) {
		super( navigable, new NavigablePath( navigable.getEmbeddedDescriptor().getRoleName() ) );

		this.sqlSelections = sqlSelections;
	}

	@Override
	public DomainResultAssembler createResultAssembler(
			Consumer<Initializer> initializerCollector,
			AssemblerCreationState creationOptions) {
		final CompositeForeignKeyInitializerImpl initializer = new CompositeForeignKeyInitializerImpl(
				null,
				this,
				initializerCollector,
				creationOptions
		);

		return new CompositeForeignKeyAssembler( initializer );
	}

	@Override
	public String getResultVariable() {
		return null;
	}

	@Override
	public EmbeddableJavaDescriptor getJavaTypeDescriptor() {
		return getCompositeNavigableDescriptor().getJavaTypeDescriptor();
	}

	@Override
	public EmbeddedValuedNavigable getCompositeNavigableDescriptor() {
		return (EmbeddedValuedNavigable) super.getNavigableContainer();
	}

	public class CompositeForeignKeyAssembler implements DomainResultAssembler {
		private final CompositeInitializer initializer;


		public CompositeForeignKeyAssembler(CompositeInitializer initializer) {
			this.initializer = initializer;
		}

		@Override
		public JavaTypeDescriptor getJavaTypeDescriptor() {
			return initializer.getEmbeddedDescriptor().getJavaTypeDescriptor();
		}

		@Override
		public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
			initializer.resolveKey( rowProcessingState );
			initializer.resolveInstance( rowProcessingState );
			initializer.initializeInstance( rowProcessingState );
			return initializer.getCompositeInstance();
		}
	}

	@Override
	protected void afterInitialize(DomainResultCreationState creationState) {

	}


	public class CompositeForeignKeyInitializerImpl extends CompositeRootInitializerImpl {

		public CompositeForeignKeyInitializerImpl(
				FetchParentAccess fetchParentAccess,
				CompositeMappingNode resultDescriptor,
				Consumer<Initializer> initializerConsumer,
				AssemblerCreationState creationOptions) {
			super( fetchParentAccess, resultDescriptor, initializerConsumer, creationOptions );
		}

		@Override
		public void initializeInstance(RowProcessingState rowProcessingState) {

			CompositeLoadingLogger.INSTANCE.debugf(
					"Initializing composite instance [%s] : %s",
					LoggingHelper.toLoggableString( getNavigablePath() ),
					getCompositeInstance()
			);

			// todo (6.0) : not sure it the correct way to initialize the embeddable
			final int size = sqlSelections.size();
			Object[] result = new Object[size];
			for ( int i = 0; i < size; i++ ) {
				result[i] = rowProcessingState.getJdbcValue( sqlSelections.get( i ) );
			}

			getEmbeddedDescriptor().setPropertyValues( getCompositeInstance(), result );
		}
	}
}
