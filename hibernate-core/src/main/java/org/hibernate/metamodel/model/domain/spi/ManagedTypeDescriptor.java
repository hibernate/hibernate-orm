/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.annotations.Remove;
import org.hibernate.boot.model.domain.spi.ManagedTypeMappingImplementor;
import org.hibernate.graph.Graph;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.ManagedJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Hibernate extension SPI for working with {@link ManagedType} implementations.  All
 * concrete ManagedType implementations (entity and embedded) are modelled as a
 * "descriptor" (see {@link EntityTypeDescriptor} and {@link EmbeddedTypeDescriptor}
 *
 * NOTE : Hibernate additionally classifies plural attributes via a "descriptor" :
 * {@link PersistentCollectionDescriptor}.
 *
 * @todo (6.0) : describe what is available after each initialization phase (and therefore what is "undefined" in terms of access earlier).
 *
 * @author Steve Ebersole
 */
public interface ManagedTypeDescriptor<T>
		extends ManagedDomainType<T>,
		SimpleTypeDescriptor<T>, NavigableContainer<T>, EmbeddedContainer<T>, ExpressableType<T>, StateArrayContributorContainer {

	/**
	 * Opportunity to perform any final tasks as part of initialization of the
	 * runtime model.  At this point...
	 *
	 * todo (6.0) : document the expectations of "at this point"
	 */
	boolean finishInitialization(
			ManagedTypeMappingImplementor bootDescriptor,
			RuntimeModelCreationContext creationContext);

	default void postInitialization(RuntimeModelCreationContext creationContext) {

	}

	TypeConfiguration getTypeConfiguration();

	ManagedJavaDescriptor<T> getJavaTypeDescriptor();

	/**
	 * The Hibernate "type name" ("entity name" - for non-POJO representations)
	 */
	default String getDomainTypeName() {
		return getJavaTypeDescriptor().getTypeName();
	}

	ManagedTypeRepresentationStrategy getRepresentationStrategy();

	/**
	 * Return this managed type's persistent attributes, including those
	 * declared on super types.
	 */
	NonIdPersistentAttribute<? super T, ?> findPersistentAttribute(String name);

	/**
	 * Return this managed type's persistent attributes, excluding those
	 * declared on super types.
	 *
	 * @apiNote See the api-note on {@link #findPersistentAttribute}
	 */
	NonIdPersistentAttribute<? super T, ?> findDeclaredPersistentAttribute(String name);

	@SuppressWarnings("unchecked")
	default <R> NonIdPersistentAttribute<? super T, R> findDeclaredPersistentAttribute(String name, Class<R> resultType) {
		return (NonIdPersistentAttribute<? super T, R>) findDeclaredPersistentAttribute( name );
	}

	List<NonIdPersistentAttribute> getPersistentAttributes();

	List<NonIdPersistentAttribute> getDeclaredPersistentAttributes();

	default void controlledVisitAttributes(Function<NonIdPersistentAttribute,Boolean> action) {
		for ( NonIdPersistentAttribute attribute : getPersistentAttributes() ) {
			final Boolean keepGoing = action.apply( attribute );
			if ( ! keepGoing ) {
				break;
			}
		}
	}

	@SuppressWarnings("unchecked")
	default <O,J> void visitAttributes(Consumer<PersistentAttributeDescriptor<O,J>> action, Predicate<PersistentAttributeDescriptor<O,J>> filter) {
		visitAttributes(
				attribute -> {
					if ( filter.test( attribute ) ) {
						action.accept( attribute );
					}
				}
		);
	}

	default void visitAttributes(Consumer<NonIdPersistentAttribute> consumer) {
		for ( NonIdPersistentAttribute attribute : getPersistentAttributes() ) {
			consumer.accept( attribute );
		}
	}

	default boolean hasMutableProperties() {
		throw new NotYetImplementedFor6Exception();
	}


	/**
	 * Set the given values to the mapped properties of the given object
	 */
	default void setPropertyValues(Object object, Object[] values) {
		// todo (6.0) : hook in BytecodeProvider's ReflectionOptimizer (if one) for this managed-type
		visitStateArrayContributors(
				contributor -> {
					final Object value = values[ contributor.getStateArrayPosition() ];
					contributor.getPropertyAccess().getSetter().set(
							object,
							value,
							getTypeConfiguration().getSessionFactory()
					);
				}
		);
	}

	/**
	 * Return the (loaded) values of the mapped properties of the object (not including backrefs)
	 */
	default Object[] getPropertyValues(Object object) {
		// todo (6.0) : hook in BytecodeProvider's ReflectionOptimizer (if one) for this managed-type
		final Object[] values = new Object[getStateArrayContributors().size()];
		visitStateArrayContributors(
				contributor ->
						values[contributor.getStateArrayPosition()] = contributor
								.getPropertyAccess().getGetter().get( object )

		);
		return values;
	}


	/**
	 * @deprecated Use the attribute's {@link org.hibernate.property.access.spi.PropertyAccess} instead
	 */
	@Remove
	@Deprecated
	default void setPropertyValue(Object object, int i, Object value) {
		throw new NotYetImplementedFor6Exception();
	}

	/**
	 * @deprecated Use the attribute's {@link org.hibernate.property.access.spi.PropertyAccess} instead
	 */
	@Remove
	@Deprecated
	default Object getPropertyValue(Object object, int i) throws HibernateException {
		// todo (6.0) : this is interesting - do we need this?
		//		It is interesting because it implies an ordering amongst the attributes.  I've discussed
		//		this with Luis a few times in regards to bytecode enhancement.  If the order were
		//				1. well defined (alphabetical, super classes first)
		//				2. ideally. easily calculable - probably a Helper class
		throw new NotYetImplementedFor6Exception();
	}

	/**
	 * @deprecated Use the attribute's {@link org.hibernate.property.access.spi.PropertyAccess} instead
	 */
	@Remove
	@Deprecated
	default Object getPropertyValue(Object object, String propertyName) {
		return findPersistentAttribute( propertyName ).getPropertyAccess().getGetter().get( object );
	}

	/**
	 * Make an empty sub-graph based on this type.
	 *
	 * @apiNote Note that this is *not* the same as the type's
	 * {@linkplain #getDefaultGraph "default" graph}
	 */
	SubGraphImplementor<T> makeSubGraph();

	/**
	 * The default graph for this type.  Generally this is used to
	 * implement JPA's notion of a "load graph" for undefined sub-graphs.
	 *
	 * @apiNote The return is immutable ({@link Graph#isMutable()} == {@code false}) -
	 * use a {@linkplain Graph#makeCopy mutable copy}
	 */
	default SubGraphImplementor<T> getDefaultGraph() {
		throw new UnsupportedOperationException( "Not yet implemented - " + getClass().getName() );
	}

	/**
	 * Make a sub-graph based on one of this type's sub-types
	 */
	<S extends T> SubGraphImplementor<S> makeSubGraph(Class<S> subType);

	<S extends T> ManagedTypeDescriptor<S> findSubType(String subTypeName);

	<S extends T> ManagedTypeDescriptor<S> findSubType(Class<S> type);

	/**
	 * In-flight access to the managed type.  Used to add attributes, etc.
	 * Valid only during boot.
	 */
	InFlightAccess<T> getInFlightAccess();

	/**
	 * Used during creation of the managed type object to add its attributes
	 */
	interface InFlightAccess<T> {
		void addAttribute(PersistentAttributeDescriptor<T, ?> attribute);

		/**
		 * Called when configuration of the managed-type is complete
		 */
		void finishUp();
	}
}
