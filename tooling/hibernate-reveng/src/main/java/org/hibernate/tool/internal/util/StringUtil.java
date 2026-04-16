/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2018-2025 Red Hat, Inc.
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
package org.hibernate.tool.internal.util;

import java.util.Objects;
import java.util.StringTokenizer;

/**
 * max: Removed methods that dependent on anything else than common.StringUtils.
 * 
 * <p>Common <code>String</code> manipulation routines.</p>
 *
 * <p>Originally from 
 * <a href="http://jakarta.apache.org/turbine/">Turbine</a> and the
 * GenerationJavaCore library.</p>
 *
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @author <a href="mailto:gcoladonato@yahoo.com">Greg Coladonato</a>
 * @author <a href="mailto:bayard@generationjava.com">Henri Yandell</a>
 * @author <a href="mailto:ed@apache.org">Ed Korthof</a>
 * @author <a href="mailto:rand_mcneely@yahoo.com>Rand McNeely</a>
 * @author <a href="mailto:scolebourne@joda.org>Stephen Colebourne</a>
 * @author <a href="mailto:fredrik@westermarck.com>Fredrik Westermarck</a>
 * @version $Id$
 */
public final class StringUtil {

    public static String[] split(String str, String separator) {
        StringTokenizer tok = null;
        if (separator == null) {
            tok = new StringTokenizer(str);
        }
        else {
            tok = new StringTokenizer(str, separator);
        }

        int listSize = tok.countTokens();
        String[] list = new String[listSize];
        int i = 0;
        int lastTokenBegin = 0;
        int lastTokenEnd = 0;
        while (tok.hasMoreTokens() ) {
            list[i] = tok.nextToken();
            lastTokenBegin = str.indexOf(list[i], lastTokenEnd);
            lastTokenEnd = lastTokenBegin + list[i].length();
            i++;
        }
        return list;
    }

    public static String leftPad(String str, int size) {
        size = (size - str.length() );
        if (size > 0) {
            str = " ".repeat(size) + str;
        }
        return str;
    }

    public static boolean isEqual(String str1, String str2) {
        return Objects.equals( str1, str2 );
    }

    public static boolean isEmptyOrNull(String string) {
        return string == null || string.isEmpty();
    }

}
