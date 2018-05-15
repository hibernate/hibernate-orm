/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryCollectionLoadReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryJoinReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryPropertyReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryScalarReturnType;
import org.hibernate.boot.jaxb.hbm.spi.NativeQueryNonScalarRootReturn;
import org.hibernate.boot.jaxb.hbm.spi.ResultSetMappingBindingDefinition;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.resultset.internal.EntityResultDefinitionImpl;
import org.hibernate.boot.model.resultset.internal.FetchDefinitionImpl;
import org.hibernate.boot.model.resultset.internal.PersistentCollectionResultDefinitionImpl;
import org.hibernate.boot.model.resultset.internal.ResultSetMappingDefinitionImpl;
import org.hibernate.boot.model.resultset.internal.ScalarResultDefinitionImpl;
import org.hibernate.boot.model.resultset.spi.ResultSetMappingDefinition;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;

/**
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public abstract class ResultSetMappingBinder {

	// todo (6.0) : we need to delay the building of the QueryResultBuilder instances until after the SessionFactory is available
	//		something like a `QueryResultBuilderDescriptor` against which we can call
	//		`#resolve(SessionFactoryImplementor factory)` or similar (what ags?)
	//
	// See https://hibernate.atlassian.net/browse/HHH-13082

	/**
	 * Build a ResultSetMappingDefinition given a containing element for the "return-XXX" elements.
	 * <p/>
	 * This form is used for ResultSet mappings defined outside the context of any specific entity.
	 * For {@code hbm.xml} this means at the root of the document.  For annotations, this means at
	 * the package level.
	 *
	 * @param hbmResultSetMapping The XML data as a JAXB binding
	 * @param context The mapping state
	 *
	 * @return The ResultSet mapping descriptor
	 */
	public static ResultSetMappingDefinition bind(
			ResultSetMappingBindingDefinition hbmResultSetMapping,
			HbmLocalMetadataBuildingContext context) {
		if ( hbmResultSetMapping.getName() == null ) {
			throw new MappingException(
					"ResultSet mapping did not specify name",
					context.getOrigin()
			);
		}

		final ResultSetMappingDefinitionImpl bootModel = new ResultSetMappingDefinitionImpl( hbmResultSetMapping.getName() );
		bind( hbmResultSetMapping, bootModel, context );
		return bootModel;
	}

	/**
	 * Build a ResultSetMappingDefinition given a containing element for the "return-XXX" elements
	 *
	 * @param resultSetMappingSource The XML data as a JAXB binding
	 * @param context The mapping state
	 * @param prefix A prefix to apply to named ResultSet mapping; this is either {@code null} for
	 * ResultSet mappings defined outside of any entity, or the name of the containing entity
	 * if defined within the context of an entity
	 *
	 * @return The ResultSet mapping descriptor
	 */
	public static ResultSetMappingDefinition bind(
			ResultSetMappingBindingDefinition resultSetMappingSource,
			HbmLocalMetadataBuildingContext context,
			String prefix) {
		if ( StringHelper.isEmpty( prefix ) ) {
			throw new AssertionFailure( "Passed prefix was null; perhaps you meant to call the alternate #bind form?" );
		}

		final String resultSetName = prefix + '.' + resultSetMappingSource.getName();
		final ResultSetMappingDefinitionImpl binding = new ResultSetMappingDefinitionImpl( resultSetName );
		bind( resultSetMappingSource, binding, context );
		return binding;
	}

	private static void bind(
			ResultSetMappingBindingDefinition hbmModel,
			ResultSetMappingDefinitionImpl bootModel,
			HbmLocalMetadataBuildingContext context) {

		for ( Object valueMappingSource : hbmModel.getValueMappingSources() ) {
			if ( JaxbHbmNativeQueryReturnType.class.isInstance( valueMappingSource ) ) {
				bootModel.addResult(
						extractReturnDescription( (JaxbHbmNativeQueryReturnType) valueMappingSource, context )
				);
			}
			else if ( JaxbHbmNativeQueryCollectionLoadReturnType.class.isInstance( valueMappingSource ) ) {
				bootModel.addResult(
						extractReturnDescription( (JaxbHbmNativeQueryCollectionLoadReturnType) valueMappingSource, context )
				);
			}
			else if ( JaxbHbmNativeQueryJoinReturnType.class.isInstance( valueMappingSource ) ) {
				bootModel.addFetch(
						extractReturnDescription( (JaxbHbmNativeQueryJoinReturnType) valueMappingSource, context )
				);
			}
			else if ( JaxbHbmNativeQueryScalarReturnType.class.isInstance( valueMappingSource ) ) {
				bootModel.addResult(
						extractReturnDescription( (JaxbHbmNativeQueryScalarReturnType) valueMappingSource, context )
				);
			}
		}
	}

	// todo : look to add query/resultset-mapping name to exception messages here.
	// 		needs a kind of "context", i.e.:
	//		"Unable to resolve type [%s] specified for native query scalar return"
	//		becomes
	//		"Unable to resolve type [%s] specified for native query scalar return as part of [query|resultset-mapping] [name]"
	//
	//		MappingException already carries origin, adding the query/resultset-mapping name pinpoints the location :)

	public static ScalarResultDefinitionImpl extractReturnDescription(
			JaxbHbmNativeQueryScalarReturnType rtnSource,
			HbmLocalMetadataBuildingContext context) {
		return new ScalarResultDefinitionImpl(
				rtnSource.getColumn(),
				rtnSource.getType()
		) {
			@Override
			protected HibernateException noTypeException(String typeName) {
				// todo (7.0) : adjust this to just use `Origin` in `ResultSetMappingBootModel.Scalar`
				//		That requires annotations binding to implement the "hierarchical context"
				//		used in hbm-binding, which will not happen until 7
				throw new MappingException(
						String.format(
								"Unable to resolve type [%s] specified for native query scalar return",
								typeName
						),
						context.getOrigin()
				);
			}
		};
	}


	public static ResultSetMappingDefinition.EntityResult extractReturnDescription(
			JaxbHbmNativeQueryReturnType rtnSource,
			HbmLocalMetadataBuildingContext context) {
		// todo (6.0) : handle attributes, discriminator, etc
		return new EntityResultDefinitionImpl(
				rtnSource.getEntityName(),
				rtnSource.getClazz(),
				rtnSource.getAlias()
		);
	}

	public static FetchDefinitionImpl extractReturnDescription(
			JaxbHbmNativeQueryJoinReturnType rtnSource,
			HbmLocalMetadataBuildingContext context) {
		final int dot = rtnSource.getProperty().lastIndexOf( '.' );
		if ( dot == -1 ) {
			throw new MappingException(
					String.format(
							Locale.ENGLISH,
							"Role attribute for sql query return [%s] not formatted correctly {owningAlias.propertyName}",
							rtnSource.getAlias()
					),
					context.getOrigin()
			);
		}

		String roleOwnerAlias = rtnSource.getProperty().substring( 0, dot );
		String roleProperty = rtnSource.getProperty().substring( dot + 1 );

		return new FetchDefinitionImpl(
				rtnSource.getAlias(),
				roleOwnerAlias,
				roleProperty ,
				rtnSource.getLockMode()
		);
	}

	public static PersistentCollectionResultDefinitionImpl extractReturnDescription(
			JaxbHbmNativeQueryCollectionLoadReturnType rtnSource,
			HbmLocalMetadataBuildingContext context) {
		final int dot = rtnSource.getRole().lastIndexOf( '.' );
		if ( dot == -1 ) {
			throw new MappingException(
					String.format(
							Locale.ENGLISH,
							"Collection attribute for sql query return [%s] not formatted correctly {OwnerClassName.propertyName}",
							rtnSource.getAlias()
					),
					context.getOrigin()
			);
		}

		return new PersistentCollectionResultDefinitionImpl(
				context.findEntityBinding( null, rtnSource.getRole().substring( 0, dot ) ).getClassName(),
				rtnSource.getRole().substring( dot + 1 ),
				rtnSource.getAlias(),
				rtnSource.getLockMode(), extractPropertyResults( rtnSource.getAlias(), rtnSource, null, context )
		);
	}

	/**
	 * return-property extraction/binding specific to an entity return to account for
	 * discriminator.
	 *
	 * @param alias The return alias
	 * @param rtnSource The entity return jaxb binding
	 * @param pc The located PersistentClass
	 * @param context The hbm context
	 *
	 * @return The extracted property mappings
	 */
	private static Map<String, String[]> extractPropertyResults(
			String alias,
			JaxbHbmNativeQueryReturnType rtnSource,
			PersistentClass pc,
			HbmLocalMetadataBuildingContext context) {
		Map<String, String[]> results = extractPropertyResults(
				alias,
				(NativeQueryNonScalarRootReturn) rtnSource,
				pc,
				context
		);

		if ( rtnSource.getReturnDiscriminator() != null ) {
			if ( results == null ) {
				results = new HashMap<>();
			}

			final String column = rtnSource.getReturnDiscriminator().getColumn();
			if ( column == null ) {
				throw new MappingException(
						String.format(
								Locale.ENGLISH,
								"return-discriminator [%s (%s)] did not specify column",
								pc.getEntityName(),
								alias
						),
						context.getOrigin()
				);
			}

			results.put( "class", new String[] {column} );
		}

		return results;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String[]> extractPropertyResults(
			String alias,
			NativeQueryNonScalarRootReturn rtnSource,
			PersistentClass pc,
			HbmLocalMetadataBuildingContext context) {
		if ( CollectionHelper.isEmpty( rtnSource.getReturnProperty() ) ) {
			return null;
		}

		final HashMap results = new HashMap();

		List<JaxbHbmNativeQueryPropertyReturnType> propertyReturnSources = new ArrayList<>();
		List<String> propertyNames = new ArrayList<>();


		for ( JaxbHbmNativeQueryPropertyReturnType propertyReturnSource : rtnSource.getReturnProperty() ) {
			final int dotPosition = propertyReturnSource.getName().lastIndexOf( '.' );
			if ( pc == null || dotPosition == -1 ) {
				propertyReturnSources.add( propertyReturnSource );
				propertyNames.add( propertyReturnSource.getName() );
			}
			else {
				final String reducedName = propertyReturnSource.getName().substring( 0, dotPosition );
				final Value value = pc.getRecursiveProperty( reducedName ).getValue();

				List<PersistentAttributeMapping> parentAttributes;
				if ( value instanceof Component ) {
					final Component comp = (Component) value;
					parentAttributes = comp.getDeclaredPersistentAttributes();
				}
				else if ( value instanceof ToOne ) {
					final ToOne toOne = (ToOne) value;
					final PersistentClass referencedPc = context.getMetadataCollector()
							.getEntityBinding( toOne.getReferencedEntityName() );
					if ( toOne.getReferencedPropertyName() != null ) {
						try {
							parentAttributes = ( (Component) referencedPc.getRecursiveProperty( toOne.getReferencedPropertyName() )
									.getValue() ).getDeclaredPersistentAttributes();
						}
						catch (ClassCastException e) {
							throw new MappingException(
									"dotted notation reference neither a component nor a many/one to one",
									e,
									context.getOrigin()
							);
						}
					}
					else {
						try {
							if ( referencedPc.getEntityMappingHierarchy()
									.getIdentifierEmbeddedValueMapping() == null ) {
								parentAttributes = ( (Component) referencedPc.getIdentifierAttributeMapping()
										.getValueMapping() ).getDeclaredPersistentAttributes();
							}
							else {
								parentAttributes = referencedPc.getEntityMappingHierarchy()
										.getIdentifierEmbeddedValueMapping()
										.getDeclaredPersistentAttributes();
							}
						}
						catch (ClassCastException e) {
							throw new MappingException(
									"dotted notation reference neither a component nor a many/one to one",
									e,
									context.getOrigin()
							);
						}
					}
				}
				else {
					throw new MappingException(
							"dotted notation reference neither a component nor a many/one to one",
							context.getOrigin()
					);
				}

				boolean hasFollowers = false;
				List<String> followers = new ArrayList<>();
				for(PersistentAttributeMapping parentAttribute : parentAttributes){
					final String currentPropertyName = parentAttribute.getName();
					final String currentName = reducedName + '.' + currentPropertyName;
					if ( hasFollowers ) {
						followers.add( currentName );
					}
					if ( propertyReturnSource.getName().equals( currentName ) ) {
						hasFollowers = true;
					}
				}

				int index = propertyNames.size();
				for ( String follower : followers ) {
					int currentIndex = getIndexOfFirstMatchingProperty( propertyNames, follower );
					index = currentIndex != -1 && currentIndex < index ? currentIndex : index;
				}
				propertyNames.add( index, propertyReturnSource.getName() );
				propertyReturnSources.add( index, propertyReturnSource );
			}
		}

		Set<String> uniqueReturnProperty = new HashSet<>();
		for ( JaxbHbmNativeQueryPropertyReturnType propertyReturnBinding : propertyReturnSources ) {
			final String name = propertyReturnBinding.getName();
			if ( "class".equals( name ) ) {
				throw new MappingException(
						"class is not a valid property name to use in a <return-property>, use <return-discriminator> instead",
						context.getOrigin()
				);
			}
			//TODO: validate existing of property with the chosen name. (secondpass )
			ArrayList allResultColumns = extractResultColumns( propertyReturnBinding );

			if ( allResultColumns.isEmpty() ) {
				throw new MappingException(
						String.format(
								Locale.ENGLISH,
								"return-property [alias=%s, property=%s] must specify at least one column or return-column name",
								alias,
								propertyReturnBinding.getName()
						),
						context.getOrigin()
				);
			}
			if ( uniqueReturnProperty.contains( name ) ) {
				throw new MappingException(
						String.format(
								Locale.ENGLISH,
								"Duplicate return-property [alias=%s] : %s",
								alias,
								propertyReturnBinding.getName()
						),
						context.getOrigin()
				);
			}
			uniqueReturnProperty.add( name );

			// the issue here is that for <return-join/> representing an entity collection,
			// the collection element values (the property values of the associated entity)
			// are represented as 'element.{propertyname}'.  Thus the StringHelper.root()
			// here puts everything under 'element' (which additionally has significant
			// meaning).  Probably what we need to do is to something like this instead:
			//      String root = StringHelper.root( name );
			//      String key = root; // by default
			//      if ( !root.equals( name ) ) {
			//	        // we had a dot
			//          if ( !root.equals( alias ) {
			//              // the root does not apply to the specific alias
			//              if ( "elements".equals( root ) {
			//                  // we specifically have a <return-join/> representing an entity collection
			//                  // and this <return-property/> is one of that entity's properties
			//                  key = name;
			//              }
			//          }
			//      }
			// but I am not clear enough on the intended purpose of this code block, especially
			// in relation to the "Reorder properties" code block above...
			//			String key = StringHelper.root( name );
			ArrayList intermediateResults = (ArrayList) results.get( name );
			if ( intermediateResults == null ) {
				results.put( name, allResultColumns );
			}
			else {
				intermediateResults.addAll( allResultColumns );
			}
		}

		for ( Object o : results.entrySet() ) {
			Map.Entry entry = (Map.Entry) o;
			if ( entry.getValue() instanceof ArrayList ) {
				ArrayList list = (ArrayList) entry.getValue();
				entry.setValue( list.toArray( new String[list.size()] ) );
			}
		}
		return results.isEmpty() ? Collections.EMPTY_MAP : results;
	}

	private static int getIndexOfFirstMatchingProperty(List propertyNames, String follower) {
		int propertySize = propertyNames.size();
		for ( int propIndex = 0; propIndex < propertySize; propIndex++ ) {
			if ( ( (String) propertyNames.get( propIndex ) ).startsWith( follower ) ) {
				return propIndex;
			}
		}
		return -1;
	}

	private static ArrayList<String> extractResultColumns(JaxbHbmNativeQueryPropertyReturnType propertyReturnSource) {
		final String column = unquote( propertyReturnSource.getColumn() );
		ArrayList<String> allResultColumns = new ArrayList<>();
		if ( column != null ) {
			allResultColumns.add( column );
		}
		for ( JaxbHbmNativeQueryPropertyReturnType.JaxbHbmReturnColumn returnColumnSource : propertyReturnSource.getReturnColumn() ) {
			allResultColumns.add( unquote( returnColumnSource.getName() ) );
		}
		return allResultColumns;
	}

	private static String unquote(String name) {
		if ( name != null && name.charAt( 0 ) == '`' ) {
			name = name.substring( 1, name.length() - 1 );
		}
		return name;
	}
}
