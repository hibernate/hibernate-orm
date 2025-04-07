/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.id.factory.spi.StandardGenerator;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.type.CompositeType;

import static org.hibernate.generator.EventType.INSERT;

/**
 * For composite identifiers, defines a number of "nested" generations that
 * need to happen to "fill" the identifier property(s).
 * <p>
 * This generator is used implicitly for all composite identifier scenarios if an
 * explicit generator is not in place.  So it make sense to discuss the various 
 * potential scenarios:<ul>
 * <li>
 * <i>"embedded" composite identifier</i> - this is possible only in HBM mappings
 * as {@code <composite-id/>} (notice the lack of both a name and class attribute
 * declarations).  The term {@link org.hibernate.mapping.Component#isEmbedded() "embedded"}
 * here refers to the Hibernate usage which is actually the exact opposite of the JPA
 * meaning of "embedded".  Essentially this means that the entity class itself holds
 * the named composite pk properties.  This is very similar to the JPA {@code @IdClass}
 * usage, though without a separate pk-class for loading.
 * </li>
 * <li>
 * <i>pk-class as entity attribute</i> - this is possible in both annotations ({@code @EmbeddedId})
 * and HBM mappings ({@code <composite-id name="idAttributeName" class="PkClassName"/>})
 * </li>
 * <li>
 * <i>"embedded" composite identifier with a pk-class</i> - this is the JPA {@code @IdClass} use case
 * and is only possible in annotations
 * </li>
 * </ul>
 * <p>
 * Most of the grunt work is done in {@link org.hibernate.mapping.Component}.
 *
 * @author Steve Ebersole
 */
@Internal
public class CompositeNestedGeneratedValueGenerator
		implements IdentifierGenerator, StandardGenerator, IdentifierGeneratorAggregator, Serializable {
	/**
	 * Contract for declaring how to locate the context for sub-value injection.
	 */
	public interface GenerationContextLocator {
		/**
		 * Given the incoming object, determine the context for injecting back its generated
		 * id sub-values.
		 *
		 * @param session The current session
		 * @param incomingObject The entity for which we are generating id
		 *
		 * @return The injection context
		 */
		Object locateGenerationContext(SharedSessionContractImplementor session, Object incomingObject);
	}

	/**
	 * Contract for performing the actual sub-value generation, usually injecting it into the
	 * determined {@linkplain GenerationContextLocator#locateGenerationContext context}
	 */
	public interface GenerationPlan extends ExportableProducer {
		/**
		 * Initializes this instance, in particular pre-generates SQL as necessary.
		 * <p>
		 * This method is called after {@link #registerExportables(Database)}, before first use.
		 *
		 * @param context A context to help generate SQL strings
		 */
		void initialize(SqlStringGenerationContext context);

		/**
		 * Retrieve the generator for this generation plan
		 */
		BeforeExecutionGenerator getGenerator();

		/**
		 * Returns the {@link Setter injector} for the generated property.
		 * Used when the {@link CompositeType} is {@linkplain CompositeType#isMutable() mutable}.
		 *
		 * @see #getPropertyIndex()
		 */
		Setter getInjector();

		/**
		 * Returns the index of the generated property.
		 * Used when the {@link CompositeType} is not {@linkplain CompositeType#isMutable() mutable}.
		 *
		 * @see #getInjector()
		 */
		int getPropertyIndex();
	}

	private final GenerationContextLocator generationContextLocator;
	private final CompositeType compositeType;
	private final List<GenerationPlan> generationPlans = new ArrayList<>();

	public CompositeNestedGeneratedValueGenerator(
			GenerationContextLocator generationContextLocator,
			CompositeType compositeType) {
		this.generationContextLocator = generationContextLocator;
		this.compositeType = compositeType;
	}

	public void addGeneratedValuePlan(GenerationPlan plan) {
		generationPlans.add( plan );
	}

	@Override
	public Object generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
		final Object context = generationContextLocator.locateGenerationContext( session, object );

		final List<Object> generatedValues = compositeType.isMutable() ?
				null :
				new ArrayList<>( generationPlans.size() );
		for ( GenerationPlan generationPlan : generationPlans ) {
			final BeforeExecutionGenerator generator = generationPlan.getGenerator();
			final Object generated;
			if ( !generator.generatedOnExecution( object, session ) ) {
				final Object currentValue = generator.allowAssignedIdentifiers()
						? compositeType.getPropertyValue( context, generationPlan.getPropertyIndex(), session )
						: null;
				generated = generator.generate( session, object, currentValue, INSERT );
			}
			else {
				throw new IdentifierGenerationException( "Identity generation isn't supported for composite ids" );
			}
			if ( generatedValues != null ) {
				generatedValues.add( generated );
			}
			else {
				generationPlan.getInjector().set( context, generated );
			}
		}

		if ( generatedValues != null) {
			final Object[] values = compositeType.getPropertyValues( context );
			for ( int i = 0; i < generatedValues.size(); i++ ) {
				values[generationPlans.get( i ).getPropertyIndex()] = generatedValues.get( i );
			}
			return compositeType.replacePropertyValues( context, values, session );
		}
		else {
			return context;
		}
	}

	@Override
	public void registerExportables(Database database) {
		for ( GenerationPlan plan : generationPlans ) {
			plan.registerExportables( database );
		}
	}

	@Override
	public void initialize(SqlStringGenerationContext context) {
		for ( GenerationPlan plan : generationPlans ) {
			plan.initialize( context );
		}
	}
}
