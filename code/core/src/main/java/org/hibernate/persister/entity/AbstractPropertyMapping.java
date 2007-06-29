//$Id: AbstractPropertyMapping.java 10852 2006-11-21 17:39:14Z steve.ebersole@jboss.com $
package org.hibernate.persister.entity;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.engine.Mapping;
import org.hibernate.sql.Template;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.StringHelper;

/**
 * Base implementation of a <tt>PropertyMapping</tt>
 *
 * @author Gavin King
 */
public abstract class AbstractPropertyMapping implements PropertyMapping {

	private final Map typesByPropertyPath = new HashMap();
	private final Map columnsByPropertyPath = new HashMap();
	private final Map formulaTemplatesByPropertyPath = new HashMap();

	public String[] getIdentifierColumnNames() {
		throw new UnsupportedOperationException("one-to-one is not supported here");
	}

	protected abstract String getEntityName();

	public Type toType(String propertyName) throws QueryException {
		Type type = (Type) typesByPropertyPath.get(propertyName);
		if ( type == null ) {
			throw propertyException( propertyName );
		}
		return type;
	}

	protected final QueryException propertyException(String propertyName) throws QueryException {
		return new QueryException( "could not resolve property: " + propertyName + " of: " + getEntityName() );
	}

	public String[] getColumnNames(String propertyName) {
		String[] cols = (String[]) columnsByPropertyPath.get(propertyName);
		if (cols==null) {
			throw new MappingException("unknown property: " + propertyName);
		}
		return cols;
	}

	public String[] toColumns(String alias, String propertyName) throws QueryException {
		//TODO: *two* hashmap lookups here is one too many...
		String[] columns = (String[]) columnsByPropertyPath.get(propertyName);
		if ( columns == null ) {
			throw propertyException( propertyName );
		}
		String[] templates = (String[]) formulaTemplatesByPropertyPath.get(propertyName);
		String[] result = new String[columns.length];
		for ( int i=0; i<columns.length; i++ ) {
			if ( columns[i]==null ) {
				result[i] = StringHelper.replace( templates[i], Template.TEMPLATE, alias );
			}
			else {
				result[i] = StringHelper.qualify( alias, columns[i] );
			}
		}
		return result;
	}

	public String[] toColumns(String propertyName) throws QueryException {
		String[] columns = (String[]) columnsByPropertyPath.get(propertyName);
		if ( columns == null ) {
			throw propertyException( propertyName );
		}
		String[] templates = (String[]) formulaTemplatesByPropertyPath.get(propertyName);
		String[] result = new String[columns.length];
		for ( int i=0; i<columns.length; i++ ) {
			if ( columns[i]==null ) {
				result[i] = StringHelper.replace( templates[i], Template.TEMPLATE, "" );
			}
			else {
				result[i] = columns[i];
			}
		}
		return result;
	}

	protected void addPropertyPath(String path, Type type, String[] columns, String[] formulaTemplates) {
		typesByPropertyPath.put(path, type);
		columnsByPropertyPath.put(path, columns);
		if (formulaTemplates!=null) {
			formulaTemplatesByPropertyPath.put(path, formulaTemplates);
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
			final String[] formulaTemplates,
			final Mapping factory)
	throws MappingException {

		if ( columns.length!=type.getColumnSpan(factory) ) {
			throw new MappingException(
					"broken column mapping for: " + path +
					" of: " + getEntityName()
				);
		}

		if ( type.isAssociationType() ) {
			AssociationType actype = (AssociationType) type;
			if ( actype.useLHSPrimaryKey() ) {
				columns = getIdentifierColumnNames();
			}
			else {
				String foreignKeyProperty = actype.getLHSPropertyName();
				if ( foreignKeyProperty!=null && !path.equals(foreignKeyProperty) ) {
					//TODO: this requires that the collection is defined after the
					//      referenced property in the mapping file (ok?)
					columns = (String[]) columnsByPropertyPath.get(foreignKeyProperty);
					if (columns==null) return; //get em on the second pass!
				}
			}
		}

		if (path!=null) addPropertyPath(path, type, columns, formulaTemplates);

		if ( type.isComponentType() ) {
			AbstractComponentType actype = (AbstractComponentType) type;
			initComponentPropertyPaths( path, actype, columns, formulaTemplates, factory );
			if ( actype.isEmbedded() ) {
				initComponentPropertyPaths(
						path==null ? null : StringHelper.qualifier(path),
						actype,
						columns,
						formulaTemplates,
						factory
					);
			}
		}
		else if ( type.isEntityType() ) {
			initIdentifierPropertyPaths( path, (EntityType) type, columns, factory );
		}
	}

	protected void initIdentifierPropertyPaths(
			final String path,
			final EntityType etype,
			final String[] columns,
			final Mapping factory) throws MappingException {

		Type idtype = etype.getIdentifierOrUniqueKeyType( factory );
		String idPropName = etype.getIdentifierOrUniqueKeyPropertyName(factory);
		boolean hasNonIdentifierPropertyNamedId = hasNonIdentifierPropertyNamedId( etype, factory );

		if ( etype.isReferenceToPrimaryKey() ) {
			if ( !hasNonIdentifierPropertyNamedId ) {
				String idpath1 = extendPath(path, EntityPersister.ENTITY_ID);
				addPropertyPath(idpath1, idtype, columns, null);
				initPropertyPaths(idpath1, idtype, columns, null, factory);
			}
		}

		if (idPropName!=null) {
			String idpath2 = extendPath(path, idPropName);
			addPropertyPath(idpath2, idtype, columns, null);
			initPropertyPaths(idpath2, idtype, columns, null, factory);
		}
	}

	private boolean hasNonIdentifierPropertyNamedId(final EntityType entityType, final Mapping factory) {
		// TODO : would be great to have a Mapping#hasNonIdentifierPropertyNamedId method
		// I don't believe that Mapping#getReferencedPropertyType accounts for the identifier property; so
		// if it returns for a property named 'id', then we should have a non-id field named id
		try {
			return factory.getReferencedPropertyType( entityType.getAssociatedEntityName(), EntityPersister.ENTITY_ID ) != null;
		}
		catch( MappingException e ) {
			return false;
		}
	}

	protected void initComponentPropertyPaths(
			final String path,
			final AbstractComponentType type,
			final String[] columns,
			String[] formulaTemplates, final Mapping factory)
	throws MappingException {

		Type[] types = type.getSubtypes();
		String[] properties = type.getPropertyNames();
		int begin=0;
		for ( int i=0; i<properties.length; i++ ) {
			String subpath = extendPath( path, properties[i] );
			try {
				int length = types[i].getColumnSpan(factory);
				String[] columnSlice = ArrayHelper.slice(columns, begin, length);
				String[] formulaSlice = formulaTemplates==null ?
						null : ArrayHelper.slice(formulaTemplates, begin, length);
				initPropertyPaths(subpath, types[i], columnSlice, formulaSlice, factory);
				begin+=length;
			}
			catch (Exception e) {
				throw new MappingException("bug in initComponentPropertyPaths", e);
			}
		}
	}

	private static String extendPath(String path, String property) {
		if ( path==null || "".equals(path) ) {
			return property;
		}
		else {
			return StringHelper.qualify(path, property);
		}
	}

}
