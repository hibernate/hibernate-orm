package org.hibernate.tool.ide.completion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author leon, max.andersen@jboss.com
 */
public class CompletionHelper {
    
    private CompletionHelper() {
    }
    
    public static String getCanonicalPath(List<EntityNameReference> qts, String name) {
        Map<String, String> alias2Type = new HashMap<String, String>();
        for (Iterator<EntityNameReference> iter = qts.iterator(); iter.hasNext();) {
			EntityNameReference qt = iter.next();
            alias2Type.put(qt.getAlias(), qt.getEntityName());
        }
        if (qts.size() == 1) { 
            EntityNameReference visible = (EntityNameReference) qts.get(0);
            String alias = visible.getAlias();
            if (name.equals(alias)) {
                return visible.getEntityName();
            } else if (alias == null || alias.length() == 0 || alias.equals(visible.getEntityName())) {
                return visible.getEntityName() + "/" + name;
            }
        }
        return getCanonicalPath(new HashSet<String>(), alias2Type, name);
    }
    
    
    private static String getCanonicalPath(Set<String> resolved, Map<String, String> alias2Type, String name) {
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
        if (isAliasNown(alias2Type, baseName)) {
            return getCanonicalPath(resolved, alias2Type, baseName) + "/" + prop;
        } else {
            return name;
        }
    }
    
    private static boolean isAliasNown(Map<String, String> alias2Type, String alias) {
        if (alias2Type.containsKey(alias)) {
            return true;
        }
        int idx = alias.lastIndexOf('.');
        if (idx == -1) {
            return false;
        }
        return isAliasNown(alias2Type, alias.substring(0, idx));
    }    
    
}
