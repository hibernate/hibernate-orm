/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.ide.completion;

import java.util.HashMap;
import java.util.HashSet;
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
        Map<String, String> alias2Type = new HashMap<>();
        for ( EntityNameReference qt : qts ) {
            alias2Type.put( qt.getAlias(), qt.getEntityName() );
        }
        if (qts.size() == 1) {
            EntityNameReference visible = qts.get(0);
            String alias = visible.getAlias();
            if (name.equals(alias)) {
                return visible.getEntityName();
            }
            else if (alias == null || alias.isEmpty() || alias.equals(visible.getEntityName())) {
                return visible.getEntityName() + "/" + name;
            }
        }
        return getCanonicalPath( new HashSet<>(), alias2Type, name);
    }


    private static String getCanonicalPath(Set<String> resolved, Map<String, String> alias2Type, String name) {
        if (resolved.contains(name)) {
            // To prevent a stack overflow
            return name;
        }
        resolved.add(name);
        String type = alias2Type.get(name);
        if (type != null) {
            return name.equals(type) ? name : getCanonicalPath(resolved, alias2Type, type);
        }
        int idx = name.lastIndexOf('.');
        if (idx == -1) {
            return name;
        }
        String baseName = name.substring(0, idx);
        String prop = name.substring(idx + 1);
        if (isAliasKnown(alias2Type, baseName)) {
            return getCanonicalPath(resolved, alias2Type, baseName) + "/" + prop;
        }
        else {
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

}
