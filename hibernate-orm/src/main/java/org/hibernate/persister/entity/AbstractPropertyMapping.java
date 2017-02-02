/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.sql.Template;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * Basic implementation of the {@link PropertyMapping} contract.
 *
 * @author Gavin King
 */
public abstract class AbstractPropertyMapping implements PropertyMapping {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( AbstractPropertyMapping.class );

	private final Map<String, Type> typesByPropertyPath = new HashMap<String, Type>();
	private final Map<String, String[]> columnsByPropertyPath = new HashMap<String, String[]>();
	private final Map<String, String[]> columnReadersByPropertyPath = new HashMap<String, String[]>();
	private final Map<String, String[]> columnReaderTemplatesByPropertyPath = new HashMap<String, String[]>();
	private final Map<String, String[]> formulaTemplatesByPropertyPath = new HashMap<String, String[]>();

	public String[] getIdentifierColumnNames() {
		throw new UnsupportedOperationException( "one-to-one is not supported here" );
	}

	public String[] getIdentifierColumnReaderTemplates() {
		throw new UnsupportedOperationException( "one-to-one is not supported here" );
	}

	public String[] getIdentifierColumnReaders() {
		throw new UnsupportedOperationException( "one-to-one is not supported here" );
	}

	protected abstract String getEntityName();

	public Type toType(String propertyName) throws QueryException {
		Type type = typesByPropertyPath.get( propertyName );
		if ( type == null ) {
			throw propertyException( propertyName );
		}
		return type;
	}

	protected final QueryException propertyException(String propertyName) throws QueryException {
		return new QueryException( "could not resolve property: " + propertyName + " of: " + getEntityName() );
	}

	public String[] getColumnNames(String propertyName) {
		String[] cols = columnsByPropertyPath.get( propertyName );
		if ( cols == null ) {
			throw new MappingException( "unknown property: " + propertyName );
		}
		return cols;
	}

	public String[] toColumns(String alias, String propertyName) throws QueryException {
		//TODO: *two* hashmap lookups here is one too many...
		String[] columns = columnsByPropertyPath.get( propertyName );
		if ( columns == null ) {
			throw propertyException( propertyName );
		}
		String[] formulaTemplates = formulaTemplatesByPropertyPath.get( propertyName );
		String[] columnReaderTemplates = columnReaderTemplatesByPropertyPath.get( propertyName );
		String[] result = new String[columns.length];
		for ( int i = 0; i < columns.length; i++ ) {
			if ( columnReaderTemplates[i] == null ) {
				result[i] = StringHelper.replace( formulaTemplates[i], Template.TEMPLATE, alias );
			}
			else {
				result[i] = StringHelper.replace( columnReaderTemplates[i], Template.TEMPLATE, alias );
			}
		}
		return result;
	}

	public String[] toColumns(String propertyName) throws QueryException {
		String[] columns = columnsByPropertyPath.get( propertyName );
		if ( columns == null ) {
			throw propertyException( propertyName );
		}
		String[] formulaTemplates = formulaTemplatesByPropertyPath.get( propertyName );
		String[] columnReaders = columnReadersByPropertyPath.get( propertyName );
		String[] result = new String[columns.length];
		for ( int i = 0; i < columns.length; i++ ) {
			if ( columnReaders[i] == null ) {
				result[i] = StringHelper.replace( formulaTemplates[i], Template.TEMPLATE, "" );
			}
			else {
				result[i] = columnReaders[i];
			}
		}
		return result;
	}

	protected void addPropertyPath(
			String path,
			Type type,
			String[] columns,
			String[] columnReaders,
			String[] columnReaderTemplates,
			String[] formulaTemplates) {
		// TODO : not quite sure yet of the difference, but this is only needed from annotations for @Id @ManyToOne support
		if ( typesByPropertyPath.containsKey( path ) ) {
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev(
						"Skipping duplicate registration of path [{0}], existing type = [{1}], incoming type = [{2}]",
						path,
						typesByPropertyPath.get( path ),
						type
				);
			}
			return;
		}
		typesByPropertyPath.put( path, type );
		columnsByPropertyPath.put( path, columns );
		columnReadersByPropertyPath.put( path, columnReaders );
		columnReaderTemplatesByPropertyPath.put( path, columnReaderTemplates );
		if ( formulaTemplates != null ) {
			formulaTemplatesByPropertyPath.put( path, formulaTemplates );
		}
	}

	/*protected void initPropertyPaths(
			final String path,
			final Type type,
			final String[] columns,
			final String[] formulaTemplates,
			final Mapping factory)
	throws MappingException {
		//addFormulaPropertyPath(path, type, formulaTemplates);
		initPropertyPaths(path, type, columns, formulaTemplates, factory);
	}*/

	protected void initPropertyPaths(
			final String path,
			final Type type,
			String[] columns,
			String[] columnReaders,
			String[] columnReaderTemplates,
			final String[] formulaTemplates,
			final Mapping factory) throws MappingException {
		assert columns != null : "Incoming columns should not be null : " + path;
		assert type != null : "Incoming type should not be null : " + path;

		if ( columns.length != type.getColumnSpan( factory ) ) {
			throw new MappingException(
					"broken column mapping for: " + path +
							" of: " + getEntityName()
			);
		}

		if ( type.isAssociationType() ) {
			AssociationType actype = (AssociationType) type;
			if ( actype.useLHSPrimaryKey() ) {
				columns = getIdentifierColumnNames();
				columnReaders = getIdentifierColumnReaders();
				columnReaderTemplates = getIdentifierColumnReaderTemplates();
			}
			else {
				String foreignKeyProperty = actype.getLHSPropertyName();
				if ( foreignKeyProperty != null && !path.equals( foreignKeyProperty ) ) {
					//TODO: this requires that the collection is defined afterQuery the
					//      referenced property in the mapping file (ok?)
					columns = columnsByPropertyPath.get( foreignKeyProperty );
					if ( columns == null ) {
						return; //get em on the second pass!
					}
					columnReaders = columnReadersByPropertyPath.get( foreignKeyProperty );
					columnReaderTemplates = columnReaderTemplatesByPropertyPath.get( foreignKeyProperty );
				}
			}
		}

		if ( path != null ) {
			addPropertyPath( path, type, columns, columnReaders, columnReaderTemplates, formulaTemplates );
		}

		if ( type.isComponentType() ) {
			CompositeType actype = (CompositeType) type;
			initComponentPropertyPaths(
					path,
					actype,
					columns,
					columnReaders,
					columnReaderTemplates,
					formulaTemplates,
					factory
			);
			if ( actype.isEmbedded() ) {
				initComponentPropertyPaths(
						path == null ? null : StringHelper.qualifier( path ),
						actype,
						columns,
						columnReaders,
						columnReaderTemplates,
						formulaTemplates,
						factory
				);
			}
		}
		else if ( type.isEntityType() ) {
			initIdentifierPropertyPaths(
					path,
					(EntityType) type,
					columns,
					columnReaders,
					columnReaderTemplates,
					factory
			);
		}
	}

	protected void initIdentifierPropertyPaths(
			final String path,
			final EntityType etype,
			final String[] columns,
			final String[] columnReaders,
			final String[] columnReaderTemplates,
			final Mapping factory) throws MappingException {

		Type idtype = etype.getIdentifierOrUniqueKeyType( factory );
		String idPropName = etype.getIdentifierOrUniqueKeyPropertyName( factory );
		boolean hasNonIdentifierPropertyNamedId = hasNonIdentifierPropertyNamedId( etype, factory );

		if ( etype.isReferenceToPrimaryKey() ) {
			if ( !hasNonIdentifierPropertyNamedId ) {
				String idpath1 = extendPath( path, EntityPersister.ENTITY_ID );
				addPropertyPath( idpath1, idtype, columns, columnReaders, columnReaderTemplates, null );
				initPropertyPaths( idpath1, idtype, columns, columnReaders, columnReaderTemplates, null, factory );
			}
		}

		if ( idPropName != null ) {
			String idpath2 = extendPath( path, idPropName );
			addPropertyPath( idpath2, idtype, columns, columnReaders, columnReaderTemplates, null );
			initPropertyPaths( idpath2, idtype, columns, columnReaders, columnReaderTemplates, null, factory );
		}
	}

	private boolean hasNonIdentifierPropertyNamedId(final EntityType entityType, final Mapping factory) {
		// TODO : would be great to have a Mapping#hasNonIdentifierPropertyNamedId method
		// I don't believe that Mapping#getReferencedPropertyType accounts for the identifier property; so
		// if it returns for a property named 'id', then we should have a non-id field named id
		try {
			return factory.getReferencedPropertyType(
					entityType.getAssociatedEntityName(),
					EntityPersister.ENTITY_ID
			) != null;
		}
		catch (MappingException e) {
			return false;
		}
	}

	protected void initComponentPropertyPaths(
			final String path,
			final CompositeType type,
			final String[] columns,
			final String[] columnReaders,
			final String[] columnReaderTemplates,
			String[] formulaTemplates, final Mapping factory) throws MappingException {

		Type[] types = type.getSubtypes();
		String[] properties = type.getPropertyNames();
		int begin = 0;
		for ( int i = 0; i < properties.length; i++ ) {
			String subpath = extendPath( path, properties[i] );
			try {
				int length = types[i].getColumnSpan( factory );
				String[] columnSlice = ArrayHelper.slice( columns, begin, length );
				String[] columnReaderSlice = ArrayHelper.slice( columnReaders, begin, length );
				String[] columnReaderTemplateSlice = ArrayHelper.slice( columnReaderTemplates, begin, length );
				String[] formulaSlice = formulaTemplates == null ?
						null : ArrayHelper.slice( formulaTemplates, begin, length );
				initPropertyPaths(
						subpath,
						types[i],
						columnSlice,
						columnReaderSlice,
						columnReaderTemplateSlice,
						formulaSlice,
						factory
				);
				begin += length;
			}
			catch (Exception e) {
				throw new MappingException( "bug in initComponentPropertyPaths", e );
			}
		}
	}

	private static String extendPath(String path, String property) {
		return StringHelper.isEmpty( path ) ? property : StringHelper.qualify( path, property );
	}
}
