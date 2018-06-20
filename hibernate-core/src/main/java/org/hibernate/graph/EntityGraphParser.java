/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graph;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Subgraph;

/**
 * A parser of {@link EntityGraph} string representations using a simple syntax.
 * <p>
 * A single graph is represented as a comma-separated list of attribute specifications, such as:
 * </p>
 * 
 * <pre>
 *   firstName, lastName, birthDate
 * </pre>
 * <p>
 * Simple attributes that are to be fetched or loaded eagerly simply need their name listed. Relational/link attributes,
 * such as *ToOne, collections and maps have an additional syntax to allow their attributes to be fetched - these are
 * specified in parentheses following the attribute name, such as:
 * </p>
 * 
 * <pre>
 *   firstName, lastName, parents(firstName, lastName, children(firstName, lastName))
 * </pre>
 * <p>
 * Maps require separate specification for their keys and values, as follows:
 * </p>
 * 
 * <pre>
 *   mapAttributeName.key(keyAttr1, keyAttr2, ....), mapAttributeName.value(valueAttr1, valueAttr2, ...)
 * </pre>
 * <p>
 * To specify additional attributes to be fetched/loaded for subclasses add the class name after the attribute name as
 * follows:
 * </p>
 * 
 * <pre>
 *   vehicles(brand, model), vehicles:Car(topSpeed), vehicles:Truck(maxCargoWeight),
 *   productOwners.key(name), productOwners.key:Swag(year),
 *   productOwners.value(firstName, lastName), productOwners.value:HistoricalUser(employmentEndDate)
 * </pre>
 * <p>
 * Above, the "Car", "Truck", "Swag" and "HistoricalUser" are class names and indicate for which subclass of the (root)
 * property/attribute class the subgraph inside the following parentheses apply to. The class names can take three
 * different forms:
 * </p>
 * <ul>
 * <li>e.g. {@code Truck} - a simple name of the class inside the same package as the property class in which scope this
 * declaration is made (the superclass). For example, if the superclass is
 * {@code com.bluecatnetworks.proteus.data.Vehicle}, then the resulting fully qualified name is
 * {@code com.bluecatnetworks.proteus.data.Truck}.</li>
 * <li>e.g. {@code .cargo.Truck} - (starting with a dot) a relative name to the package of the property class in which
 * scope this declaration is made. For example, if the superclass is {@code com.bluecatnetworks.proteus.data.Vehicle},
 * then the resulting fully qualified name is {@code com.bluecatnetworks.proteus.data.cargo.Truck}.</li>
 * <li>e.g. {@code org.acme.Truck} - (containing dots but not starting with a dot) a fully qualified class name.</li>
 * </ul>
 * <p>
 * To get an EntityGraph from a text form as per above, please use the
 * {@link #parse(EntityManager, Class, CharSequence)} method. For example:
 * </p>
 * <code>
 *   {@link EntityGraph EntityGraph}&lt;Person&gt; personGraph = EntityGraphParser.{@link #parse(EntityManager, Class, CharSequence) parse}(entityManager, Person.class, "firstName, lastName");
 * </code>
 * <p>
 * Multiple graphs made for the same entity type can be merged if it is necessary to satisfy multiple clients with the
 * same query. Gather all the individual entity graphs, then merge them using
 * {@link EntityGraphs#merge(EntityManager, Class, EntityGraph...)}.
 * </p>
 * <p>
 * For additional convenience please use methods of {@link EntityGraphs}. They allow easier configuration of
 * JPA/Hibernate queries and even embedding the above fetch/load graph text representation inside JPQL/HQL queries. See:
 * </p>
 * <ul>
 * <li>{@link EntityGraphs#createQuery(EntityManager, Class, CharSequence)}</li>
 * <li>{@link EntityGraphs#createNamedQuery(EntityManager, Class, String, CharSequence)}</li>
 * <li>{@link EntityGraphs#find(EntityManager, Class, Object, CharSequence)}</li>
 * <li>{@link FetchAndLoadEntityGraphs#applyTo(javax.persistence.Query)}</li>
 * <li>{@link FetchAndLoadEntityGraphs#list(org.hibernate.Session, org.hibernate.Query)}</li>
 * <li>{@link FetchAndLoadEntityGraphs#scroll(org.hibernate.Session, org.hibernate.Query)}</li>
 * <li>{@link FetchAndLoadEntityGraphs#iterate(org.hibernate.Session, org.hibernate.Query)}</li>
 * <li>{@link FetchAndLoadEntityGraphs#run(org.hibernate.Session, java.util.function.Supplier)}</li>
 * </ul>
 * 
 * @author asusnjar
 */
public final class EntityGraphParser {

	/**
	 * Parses and returns the textual graph representation as {@linkplain EntityGraphParser described above}.
	 * 
	 * @param <T>       The root entity type of the graph.
	 * 
	 * @param em        JPA EntityManager to use to create the graph.
	 * @param rootType  Root entity type of the graph.
	 * @param text      Textual representation to parse.
	 * 
	 * @return A parsed EntityGraph.
	 * 
	 * @throw InvalidGraphException if the textual representation is invalid.
	 */
	public static <T> EntityGraph<T> parse(final EntityManager em, final Class<T> rootType, final CharSequence text) {
		EntityGraph<T> graph = null;
		if ( text != null ) {
			graph = em.createEntityGraph( rootType );
			parseInto( graph, rootType, text );
		}
		return graph;
	}
	
	/**
	 * Parses and returns the textual graph representation as {@linkplain EntityGraphParser described above}.
	 * 
	 * @param <T>       The root entity type of the graph.
	 * 
	 * @param em        JPA EntityManager to use to create the graph.
	 * @param rootType  Root entity type of the graph.
	 * @param text      Textual representation to parse.
	 * 
	 * @return A parsed EntityGraph.
	 * 
	 * @throw InvalidGraphException if the textual representation is invalid.
	 */
	private static <T> EntityGraph<T> parse(final EntityManager em, final Class<T> rootType, final ParseBuffer buffer) {
		EntityGraph<T> graph = null;
		if ( buffer != null ) {
			graph = em.createEntityGraph( rootType );
			parseInto( buffer, subgraphFromGraph( rootType, graph ) );
		}
		return graph;
	}
	
	/**
	 * Parses the textual graph representation as {@linkplain EntityGraphParser described above}
	 * into the specified graph.
	 * 
	 * @param <T>       The root entity type of the graph.
	 * 
	 * @param graph     JPA EntityGraph to parse into.
	 * @param rootType  Root entity type of the graph.
	 * @param text      Textual representation to parse.
	 * 
	 * @throw InvalidGraphException if the textual representation is invalid.
	 */
	public static <T> void parseInto(EntityGraph<T> graph, final Class<T> rootType, final CharSequence text) {
		if ( text != null ) {
			parseInto( subgraphFromGraph( rootType, graph ), text );
		}
	}
	
	/**
	 * Parses the textual graph representation as {@linkplain EntityGraphParser described above}
	 * into the specified graph.
	 * 
	 * @param <T>       The root entity type of the subgraph.
	 * 
	 * @param graph     JPA EntityGraph to parse into.
	 * @param rootType  Root entity type of the graph.
	 * @param text      Textual representation to parse.
	 * 
	 * @throw InvalidGraphException if the textual representation is invalid.
	 */
	public static <T> void parseInto(Subgraph<T> subgraph, final CharSequence text) {
		if ( text != null ) {
			parseInto( new ParseBuffer( text ), subgraph );
		}
	}

	/**
	 * Parses the parse buffer into the specified subgraph.
	 * 
	 * @param <T>             The root entity type of the subgraph.
	 * 
	 * @param buffer          Buffer to parse.
	 * @param targetSubgraph  Subgraph to parse into.
	 * 
	 * @throw InvalidGraphException if the textual representation is invalid.
	 */
	private static <T> void parseInto(ParseBuffer buffer, Subgraph<T> targetSubgraph) {
		boolean expectSeparator = false;
		while ( !buffer.isAtEnd() ) {
			if ( expectSeparator ) {
				buffer.skipWhitespace();
				if ( !buffer.match( ',' ) ) {
					break;
				}
			}
			else {
				expectSeparator = true;
			}
			buffer.skipWhitespace();
			final String propertyIdentifier = buffer.consumeIdentifier( false );
			if ( propertyIdentifier == null ) {
				break;
			}

			buffer.skipWhitespace();

			boolean mapKey, mapValue;

			if ( buffer.match( "." ) ) {
				buffer.skipWhitespace();
				mapKey = buffer.match( "keys" ) || buffer.match( "key" );
				mapValue = mapKey ? false : ( buffer.match( "values" ) || buffer.match( "value" ) );
			}
			else {
				mapKey = mapValue = false;
			}

			// Check if there is a subgraph for this
			buffer.skipWhitespace();
			Class<? extends T> subclass = null;
			if ( buffer.match( ':' ) ) {
				buffer.skipWhitespace();
				String subclassName = buffer.consumeIdentifier( true );
				int firstDotPosition = subclassName.indexOf( '.' );
				if ( firstDotPosition < 0 ) {
					// No package specified at all, assume the same as of the owner entity
					subclassName = targetSubgraph.getClassType().getPackage().getName() + "." + subclassName;
				}
				else if ( firstDotPosition == 0 ) {
					// A subpackage of the owner entity's package specified
					subclassName = targetSubgraph.getClassType().getPackage().getName() + subclassName;
				}
				subclass = getSubclassByName( targetSubgraph.getClassType(), subclassName );
				buffer.skipWhitespace();
			}

			if ( buffer.match( '(' ) ) {
				// Yes, a collection subgraph
				final Subgraph<? extends T> subgraph;
				if ( mapKey ) {
					if ( subclass == null ) {
						subgraph = targetSubgraph.addKeySubgraph( propertyIdentifier );
					}
					else {
						subgraph = targetSubgraph.addKeySubgraph( propertyIdentifier, subclass );
					}
				}
				else {
					if ( subclass == null ) {
						subgraph = targetSubgraph.addSubgraph( propertyIdentifier );
					}
					else {
						subgraph = targetSubgraph.addSubgraph( propertyIdentifier, subclass );
					}
				}
				parseInto( buffer, subgraph );
				buffer.skipWhitespace();
				if ( !buffer.match( ')' ) ) {
					throw new InvalidGraphException( "Unclosed subgraph found near character " + buffer.getPosition() );
				}
			}
			else {
				// A simple attribute?
				if ( subclass != null ) {
					throw new InvalidGraphException( "Subclass specified for an attribute without a subgraph near character " + buffer.getPosition() );
				}
				if ( mapKey ) {
					throw new InvalidGraphException( "A map key without a subgraph near character " + buffer.getPosition() );
				}
				if ( mapValue ) {
					throw new InvalidGraphException( "A map value without a subgraph near character " + buffer.getPosition() );
				}
				targetSubgraph.addAttributeNodes( propertyIdentifier );
			}
		}
	}

	/**
	 * Returns the named subclass of the specified superclass if it is available and 
	 * is indeed a subclass, throws an {@link InvalidGraphException} otherwise.
	 * 
	 * @param <T>          The superclass.
	 * 
	 * @param superclass   (Super)class that the named subclass must be a subclass of.
	 * @param subclassName Name of the subclass to find.
	 * 
	 * @return A valid subclass of {@code superclass} with the specified name.
	 * 
	 * @throws InvalidGraphException if the class is not found or is not a subclass of
	 * {@code superclass}.
	 */
	@SuppressWarnings("unchecked")
	private static <T> Class<T> getSubclassByName(Class<T> superclass, String subclassName) {
		Class<T> subclass = null;
		try {
			subclass = (Class<T>) Class.forName( subclassName );
		}
		catch (ClassNotFoundException e) {
			throw new InvalidGraphException( "Unavailable subclass specified: " + subclassName, e );
		}

		if ( !superclass.isAssignableFrom( subclass ) ) {
			throw new InvalidGraphException( "Specified class (" + subclassName + ") must be but is not a subclass of " + superclass.getName() );
		}
		return subclass;
	}

	/**
	 * Creates a {@link Subgraph}-like proxy of the specified {@link EntityGraph}.
	 * This helps reuse the code as {@link Subgraph} and {@link EntityGraph}, unfortunately, 
	 * do not share declarations yet are almost identical.
	 * 
	 * @param <T>      The root entity type.
	 * 
	 * @param rootType Root entity type of the graph.
	 * @param graph    Graph to wrap into the new proxy.
	 * 
	 * @return A {@link Subgraph}-like proxy of the specified {@link EntityGraph}.
	 */
	private static <T> Subgraph<T> subgraphFromGraph(final Class<T> rootType, final EntityGraph<T> graph) {
		return new GraphAsSubgraph<T>( graph, rootType );
	}

	/**
	 * Parse the fetch and load textual graphs representations that are at the beginning of the
	 * JPQL or HQL query and returns the pair.
	 * 
	 * <p>
	 * The textual representations of the graphs are the same as {@linkplain EntityGraphParser described above}
	 * but add extra structure. Specifically, the graphs must be preceeded with the keyword "fetch" and/or "load".
	 * This allows the parser to disambiguate the graphs but also to detect cases when no graphs are specified
	 * at all and we have a plain JPQL/HQL query.
	 * </p>
	 * <p>
	 * For example:
	 * </p>
	 * <ul>
	 *   <li>{@code fetch name, description [select ...] from ...} contains a <i>fetch</i> graph specifying
	 *       "name" and "description" attributes.</li>
	 *   <li>{@code load name, description [select ...] from ...} contains a <i>load</i> graph specifying
	 *       "name" and "description" attributes.</li>
	 *   <li>{@code fetch name load description [select ...] from ...} contains a <i>fetch</i> graph specifying
	 *       only the "name" and a <i>load</i> graph specifying the "description" attribute.</li>
	 *   <li>{@code load description fetch name [select ...] from ...} is equivalent to previous.</li>
	 *   <li>{@code fetch name fetch description [select ...] from ...} is invalid because it specifies two
	 *       fetch graphs.</li>
	 *   <li>{@code load name load description [select ...] from ...} is invalid because it specifies two
	 *       load graphs.</li>
	 * </ul>
	 * <p>
	 * The parser stops short (before) the actual JPQL/HQL portion and leaves it in the parse buffer so
	 * that it can be passed to the corresponding query parsing/creation methods. It makes no attempt to
	 * understand, check or reconcile the fetch/load graph specifications with the query in part due to
	 * the fact that JPA standard is ambiguous as to how the FetchGraph and LoadGraph hints are applied
	 * to multiple items in the {@code select} clause.
	 * </p>
	 * 
	 * @param <T>      The root entity type.
	 * 
	 * @param em       Entity manager to use to create the graphs.
	 * @param rootType Root entity type for the graphs.
	 * @param buffer   Parse buffer to use parse from.
	 * 
	 * @return A {@link FetchAndLoadEntityGraphs} containing a pair of parsed graphs (fetch, load).
	 * If any of the graphs is not declared/included it will be {@code null}.
	 * 
	 * @throw InvalidGraphException if the textual representation is invalid.
	 */
	static <T> FetchAndLoadEntityGraphs<T> parsePreQueryGraphDescriptors(final EntityManager em, Class<T> rootType, ParseBuffer buffer) {
		EntityGraph<T> fetchGraph = null;
		EntityGraph<T> loadGraph = null;

		buffer.skipWhitespace();
		boolean foundSomething;
		do {
			String word = buffer.consumeIdentifier( false );
			if ( "fetch".equalsIgnoreCase( word ) ) {
				buffer.skipWhitespace();
				foundSomething = true;
				fetchGraph = parse( em, rootType, buffer );
				buffer.skipWhitespace();
			}
			else if ( "load".equalsIgnoreCase( word ) ) {
				buffer.skipWhitespace();
				foundSomething = true;
				loadGraph = parse( em, rootType, buffer );
				buffer.skipWhitespace();
			}
			else {
				if ( word != null) {
					buffer.back( word );
				}
				foundSomething = false;
			}
		} while ( foundSomething && ( ( fetchGraph == null ) || ( loadGraph == null ) ) );

		return new FetchAndLoadEntityGraphs<T>( fetchGraph, loadGraph );
	}
}
