/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.id;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.engine.spi.SessionImplementor;

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
	public static interface GenerationContextLocator {
		/**
		 * Given the incoming object, determine the context for injecting back its generated
		 * id sub-values.
		 *
		 * @param session The current session
		 * @param incomingObject The entity for which we are generating id
		 *
		 * @return The injection context
		 */
		public Serializable locateGenerationContext(SessionImplementor session, Object incomingObject);
	}

	/**
	 * Contract for performing the actual sub-value generation, usually injecting it into the
	 * determined {@link GenerationContextLocator#locateGenerationContext context}
	 */
	public static interface GenerationPlan extends ExportableProducer {
		/**
		 * Execute the value generation.
		 *
		 * @param session The current session
		 * @param incomingObject The entity for which we are generating id
		 * @param injectionContext The context into which the generated value can be injected
		 */
		public void execute(SessionImplementor session, Object incomingObject, Object injectionContext);
	}

	private final GenerationContextLocator generationContextLocator;
	private List<GenerationPlan> generationPlans = new ArrayList<GenerationPlan>();

	public CompositeNestedGeneratedValueGenerator(GenerationContextLocator generationContextLocator) {
		this.generationContextLocator = generationContextLocator;
	}

	public void addGeneratedValuePlan(GenerationPlan plan) {
		generationPlans.add( plan );
	}

	@Override
	public Serializable generate(SessionImplementor session, Object object) throws HibernateException {
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
}
