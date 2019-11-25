/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identity.CompositeGeneratedIdentifierReturningDelegate;
import org.hibernate.dialect.identity.CompositeGeneratedIdentifierSelectingDelegate;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.type.Type;

/**
 * For composite identifiers, defines a number of "nested" generations that
 * need to happen to "fill" the identifier property(s).
 * <p/>
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
 * <p/>
 * Most of the grunt work is done in {@link org.hibernate.mapping.Component}.
 *
 * @author Steve Ebersole
 */
public class CompositeNestedGeneratedValueGenerator implements IdentifierGenerator, Serializable, IdentifierGeneratorAggregator {
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
		Serializable locateGenerationContext(SharedSessionContractImplementor session, Object incomingObject);
	}

	/**
	 * Contract for performing the actual sub-value generation, usually injecting it into the
	 * determined {@link GenerationContextLocator#locateGenerationContext context}
	 */
	public interface GenerationPlan extends ExportableProducer {
		/**
		 * Execute the value generation.
		 *
		 * @param session The current session
		 * @param incomingObject The entity for which we are generating id
		 * @param injectionContext The context into which the generated value can be injected
		 */
		void execute(SharedSessionContractImplementor session, Object incomingObject, Object injectionContext);
	}

	public interface PostInsertGenerationPlan {

		String columnName();

		Type type();

		/**
		 * Set the post-insert generated value.
		 *
		 * @param session The current session
		 * @param generated The post insert generated value
		 * @param injectionContext The context into which the generated value can be injected
		 */
		void set(SharedSessionContractImplementor session, Serializable generated, Object injectionContext);
	}

	private final GenerationContextLocator generationContextLocator;

	private final List<GenerationPlan> generationPlans = new ArrayList<>();
	private final List<PostInsertGenerationPlan> postInsertGenerationPlans = new ArrayList<>();

	public CompositeNestedGeneratedValueGenerator(GenerationContextLocator generationContextLocator) {
		this.generationContextLocator = generationContextLocator;
	}

	public void addGeneratedValuePlan(GenerationPlan plan) {
		generationPlans.add( plan );
	}

	public void addPostInsertGeneratedValuePlan(PostInsertGenerationPlan plan) {
		postInsertGenerationPlans.add( plan );
	}

	@Override
	public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
		final Serializable context = generationContextLocator.locateGenerationContext( session, object );

		for ( Object generationPlan : generationPlans ) {
			final GenerationPlan plan = (GenerationPlan) generationPlan;
			plan.execute( session, object, context );
		}

		return context;
	}

	@Override
	public void registerExportables(Database database) {
		for (GenerationPlan plan : generationPlans) {
			plan.registerExportables( database );
		}
	}

	public boolean hasPostInsertGeneratedValue() {
		return !postInsertGenerationPlans.isEmpty();
	}

	public InsertGeneratedIdentifierDelegate getInsertGeneratedIdentifierDelegate(AbstractEntityPersister persister, Dialect dialect,
			boolean isGetGeneratedKeysEnabled) {
		if ( isGetGeneratedKeysEnabled || dialect.getIdentityColumnSupport().supportsInsertSelectIdentity() ) {
			// Use the ReturningDelegate form:
			// using the result set returning from the insert to extract the generated values
			return new CompositeGeneratedIdentifierReturningDelegate( persister, dialect, new HashSet<>( postInsertGenerationPlans ) );
		}

		// Use the SelectingDelegate form:
		// using a further select to load the generated values
		return new CompositeGeneratedIdentifierSelectingDelegate( persister, dialect, new HashSet<>( postInsertGenerationPlans ) );
	}
}
