/*
 * Envers. http://www.jboss.org/envers
 *
 * Copyright 2008  Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT A WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.tools;

import java.util.Iterator;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class StringTools {
    public static boolean isEmpty(String s) {
        return s == null || "".equals(s);
    }

    /**
     * @param s String, from which to get the last component.
     * @return The last component of the dot-separated string <code>s</code>. For example, for a string
     * "a.b.c", the result is "c".
     */
    public static String getLastComponent(String s) {
        if (s == null) {
            return null;
        }

        int lastDot = s.lastIndexOf(".");
        if (lastDot == -1) {
            return s;
        } else {
            return s.substring(lastDot + 1);
        }
    }

    /**
     * To the given string builder, appends all strings in the given iterator, separating them with the given
     * separator. For example, for an interator "a" "b" "c" and separator ":" the output is "a:b:c".
     * @param sb String builder, to which to append.
     * @param contents Strings to be appended.
     * @param separator Separator between subsequent content.
     */
    public static void append(StringBuilder sb, Iterator<String> contents, String separator) {
        boolean isFirst = true;

        while (contents.hasNext()) {
            if (!isFirst) {
                sb.append(separator);
            }

            sb.append(contents.next());
            
            isFirst = false;
        }
    }
}
