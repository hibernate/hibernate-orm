package org.hibernate.tool.ide.completion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.tool.internal.export.pojo.Cfg2JavaTool;
import org.hibernate.tool.internal.export.pojo.EntityPOJOClass;

/**
 * @author Max Rydahl Andersen
 *
 */
public class ConfigurationCompletion {

	private final Metadata metadata;

	public ConfigurationCompletion(Metadata md) {
		this.metadata = md;
	}

	public void getMatchingImports(String prefix , IHQLCompletionRequestor collector) {
		getMatchingImports( prefix, prefix.length() , collector );
	}
	
	public void getMatchingImports(String prefix, int cursorPosition, IHQLCompletionRequestor collector) {
		Iterator<Entry<String, String>> iterator = metadata.getImports().entrySet().iterator();
		while ( iterator.hasNext() ) {
			Entry<String, String> entry = iterator.next();
			String entityImport = (String) entry.getKey();
			String entityName = (String) entry.getValue();
			
			if(entityImport.toLowerCase().startsWith(prefix.toLowerCase())) {
				HQLCompletionProposal proposal = createStartWithCompletionProposal( prefix, cursorPosition, HQLCompletionProposal.ENTITY_NAME, entityImport );
				proposal.setShortEntityName( entityImport );
				proposal.setEntityName( entityName );
				collector.accept(proposal);				
								
			}
		}		
	}
	
	public void getMatchingKeywords(String prefix, int cursorPosition, IHQLCompletionRequestor collector) {
		findMatchingWords( cursorPosition, prefix, HQLAnalyzer.getHQLKeywords(), HQLCompletionProposal.KEYWORD, collector);
	}

	public void getMatchingFunctions(String prefix, int cursorPosition, IHQLCompletionRequestor collector) {
		findMatchingWords( cursorPosition, prefix, HQLAnalyzer.getHQLFunctionNames(), HQLCompletionProposal.FUNCTION, collector);
	}
	
	public void getMatchingProperties(String path, String prefix, IHQLCompletionRequestor hcc) {
		getMatchingProperties( path, prefix, prefix.length(), hcc );
	}
	
	public void getMatchingProperties(String path, String prefix, int cursorPosition, IHQLCompletionRequestor hcc) {
		int idx = path.indexOf('/');
        if (idx == -1) { // root name
            PersistentClass cmd = getPersistentClass(path);
            if (cmd == null) {
                return;
            }
            addPropertiesToList(cmd, prefix, cursorPosition, hcc);            
        } else {
            String baseEntityName = path.substring(0, idx);
            String propertyPath = path.substring(idx + 1);
            Value value = getNextAttributeType(baseEntityName, propertyPath);
            if (value == null) {
                return;
            }
            
            // Go to the next property (get the y of x/y/z when root is x)
            idx = propertyPath.indexOf('/');
            if (idx == -1) {
                path = "";
            } else {
                path = propertyPath.substring(idx + 1);
            }
            if (path.length() == 0) {
                // No properties left
                if (value instanceof Component) {
                    addPropertiesToList((Component) value, prefix, cursorPosition, hcc);
                } else if (value instanceof Collection && ((Collection)value).getElement() instanceof Component) {
                	addPropertiesToList((Component) ((Collection)value).getElement(), prefix, cursorPosition, hcc);
                } else {
                	 addPropertiesToList(getPersistentClass( getReferencedEntityName( value ) ), prefix, cursorPosition, hcc);
                }
            } else {
                // Nested properties
                if (value instanceof Component) {
                    // We need to find the first non-component type 
                    while (value instanceof Component && path.length() > 0) {
                        value = getNextAttributeType((Component) value, path);
                        if (value != null) {
                            // Consume part of the canonical path
                            idx = path.indexOf('/');
                            if (idx != -1) {
                                path = path.substring(idx + 1);
                            } else {
                                path = "";
                            }
                        }
                    }
                    if (value instanceof Component) {
                        addPropertiesToList((Component) value, prefix, cursorPosition, hcc);
                    } else if (value != null) {
                        if (path.length() > 0) {
                            path = getReferencedEntityName( value ) + "/" + path;
                        } else {
                            path = getReferencedEntityName( value );
                        }
                        getMatchingProperties( path, prefix, cursorPosition, hcc );
                    }
                } else {
                    // Just call the method recursively to add our new type
                    getMatchingProperties(getReferencedEntityName( value ) + "/" + path, prefix, cursorPosition, hcc);
                }
            }
        }
	}

	private String getReferencedEntityName(Value value) {
		if(value instanceof ToOne) {
			return ((ToOne)value).getReferencedEntityName();
		}
		if ( value instanceof Collection ) {
			Collection collection = ((Collection)value);
			Value element = collection.getElement();
			String elementType = getReferencedEntityName( element );
			if(collection.isIndexed()) {
				//TODO..list/map
				/*IndexedCollection idxCol = (IndexedCollection) collection;
				if(!idxCol.isList()) {
					Value idxElement = idxCol.getIndex();
					String indexType = getReferencedEntityName( value );
					genericDecl = indexType + "," + elementType;
				}*/
			} 			
			return elementType;
		}
		
		if(value instanceof OneToMany) {
			return ((OneToMany)value).getReferencedEntityName();
		}
		
		return null;
	}

	private void addPropertiesToList(PersistentClass cmd, String prefix, int cursorPosition, IHQLCompletionRequestor hcc) {
		if (cmd == null) {
            return;
        }
        if (prefix == null) {
            prefix = "";
        }
        
        // Add superclass's properties too
        while (cmd != null){
        	EntityPOJOClass pc = new EntityPOJOClass(cmd, new Cfg2JavaTool()); // TODO: we should extract the needed functionallity from this hbm2java class.
            
        	Iterator<Property> allPropertiesIterator = pc.getAllPropertiesIterator();
            while ( allPropertiesIterator.hasNext() ) {
    			Property property = allPropertiesIterator.next();
    			String candidate = property.getName();
    		    if (prefix.length() == 0 || candidate.toLowerCase().startsWith(prefix.toLowerCase())) {
    		    	HQLCompletionProposal proposal = createStartWithCompletionProposal( prefix, cursorPosition, HQLCompletionProposal.PROPERTY, candidate );
    		    	proposal.setEntityName( cmd.getEntityName() );
    		    	proposal.setProperty( property );
    		    	proposal.setPropertyName( candidate );		    	
    				hcc.accept( proposal);		    	                
                }
            }
            cmd = cmd.getSuperclass();
        }
           	
	}

	private HQLCompletionProposal createStartWithCompletionProposal(String prefix, int cursorPosition, int kind, String candidate) {
		HQLCompletionProposal proposal = new HQLCompletionProposal(kind, cursorPosition);
		if(candidate.startsWith(prefix)) {
			proposal.setCompletion( candidate.substring(prefix.length()) );
			proposal.setSimpleName( candidate );
			proposal.setReplaceStart( cursorPosition );	
			proposal.setReplaceEnd( cursorPosition );
		} else {
			proposal.setCompletion( candidate );
			proposal.setSimpleName( candidate );
			proposal.setReplaceStart( cursorPosition  - prefix.length() );// replace prefix	
			proposal.setReplaceEnd( cursorPosition ); 	
		}
		return proposal;
	}

	/** returns PersistentClass for path. Can be null if path is an imported non-mapped class */
	private PersistentClass getPersistentClass(String path) {
		if(path==null) return null;
		String entityName = (String) metadata.getImports().get( path );
		if(entityName==null) {
			return metadata.getEntityBinding(path);
		} else {
			return metadata.getEntityBinding(entityName);
		}	
	}

	public String getCanonicalPath(List<EntityNameReference> qts, String name) {
		Map<String, String> alias2Type = new HashMap<String, String>();
        for (Iterator<EntityNameReference> iter = qts.iterator(); iter.hasNext();) {
			EntityNameReference qt = iter.next();
            alias2Type.put(qt.getAlias(), qt.getEntityName());
        }
        if (qts.size() == 1) { 
            EntityNameReference visible = qts.get(0);
            String alias = visible.getAlias();
            if (name.equals(alias)) {
                return visible.getEntityName();
            } else if (alias == null || alias.length() == 0 || alias.equals(visible.getEntityName())) {
                return visible.getEntityName() + "/" + name;
            }
        }
        return getCanonicalPath(new HashSet<String>(), alias2Type, name);		
	}
	
	private String getCanonicalPath(Set<String> resolved, Map<String, String> alias2Type, String name) {
        if (resolved.contains(name)) {
            // To prevent a stack overflow
            return name;
        }
        resolved.add(name);
        String type = (String) alias2Type.get(name);
        if (type != null) {
            return name.equals(type) ? name : getCanonicalPath(resolved, alias2Type, type);
        }
        int idx = name.lastIndexOf('.');
        if (idx == -1) {
            return type != null ? type : name;
        }
        String baseName = name.substring(0, idx);
        String prop = name.substring(idx + 1);
        if (isAliasKnown(alias2Type, baseName)) {
            return getCanonicalPath(resolved, alias2Type, baseName) + "/" + prop;
        } else {
            return name;
        }
    }
	
	private static boolean isAliasKnown(Map<String, String> alias2Type, String alias) {
        if (alias2Type.containsKey(alias)) {
            return true;
        }
        int idx = alias.lastIndexOf('.');
        if (idx == -1) {
            return false;
        }
        return isAliasKnown(alias2Type, alias.substring(0, idx));
    }
    
	private Value getNextAttributeType(String type, String attributePath) {
        PersistentClass cmd = getPersistentClass( type );
        if (cmd == null) {
            return null;
        }
        String attribute;
        int idx = attributePath.indexOf('/');
        if (idx == -1) {
            attribute = attributePath;
        } else {
            attribute = attributePath.substring(0, idx);
        }
        
        String idName = cmd.getIdentifierProperty()==null?null:cmd.getIdentifierProperty().getName();
        if (attribute.equals(idName)) {
            return cmd.getIdentifierProperty().getValue();
        }
        try {
        	Property property = cmd.getProperty( attribute );
        	return property==null?null:property.getValue();
        } catch (HibernateException he) {
        	return null;
        }
                
    }

	private Value getNextAttributeType(Component t, String attributeName) {
        int idx = attributeName.indexOf('/');
        if (idx != -1) {
            attributeName = attributeName.substring(0, idx);
        }
        Iterator<?> names = t.getPropertyIterator();
        while ( names.hasNext() ) {
			Property element = (Property) names.next();
			String name = element.getName();
			if (attributeName.equals(name)) {
                return element.getValue();
            }
        }
        return null;
    }
	
	void addPropertiesToList(Component t, String prefix, int cursorPosition, IHQLCompletionRequestor hcc) {
        if (t == null) {
            return;
        }
        Iterator<?> props = t.getPropertyIterator();
        while ( props.hasNext() ) {
			Property element = (Property) props.next();			
			String candidate = element.getName();
			if (candidate.toLowerCase().startsWith(prefix.toLowerCase())) {
				HQLCompletionProposal proposal = createStartWithCompletionProposal( prefix, cursorPosition, HQLCompletionProposal.PROPERTY, candidate );
				//proposal.setEntityName( cmd.getEntityName() ); ...we don't know here..TODO: pass in the "path"
		    	proposal.setPropertyName( candidate );
		    	proposal.setProperty(element);
				hcc.accept( proposal);				               
            }
        }
    }
	
	private void findMatchingWords(int cursorPosition, String prefix, String[] words, int kind, IHQLCompletionRequestor hcc) {
		int i = Arrays.binarySearch(words, prefix.toLowerCase());
		if(i<0) {
			i = Math.abs(i+1);
		}
		
		for (int cnt = i; cnt < words.length; cnt++) {
			String word = words[cnt];
			if(word.toLowerCase().startsWith(prefix.toLowerCase())) {
				HQLCompletionProposal proposal = createStartWithCompletionProposal( prefix, cursorPosition, kind, word );
				hcc.accept( proposal);				
			} else {
				break;
			}
		}
	}

}
