package org.hibernate.tool.internal.util;

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
            StringBuffer buffer = new StringBuffer(size);
            for (int i = 0; i < size; i++) {
                buffer.append(" ");
            }
            str = buffer.toString() + str;
        }
        return str;
    }

	public static boolean isNotEmpty(String string) {
		return string != null && string.length() > 0;
	}

}
