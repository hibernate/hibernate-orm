package org.hibernate.tool.hbm2x;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

import java.util.Iterator;
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
public final class StringUtils {

    private StringUtils() {
    	 // noop
    }

    // Empty
    //--------------------------------------------------------------------------

    /**
     * Removes control characters, including whitespace,  from both ends of this 
     * string, handling null by returning an empty string.
     *
     * @see java.lang.String#trim()
     * @param str  the string to check
     * @return the trimmed text (never <code>null</code>)
     */
    public static String clean(String str) {
        return (str == null ? "" : str.trim() );
    }

    /**
     * Removes control characters,  including whitespace, from both ends of this
     * string, handling null by returning null. 
     *
     * @see java.lang.String#trim()
     * @param str  the string to check
     * @return the trimmed text (or <code>null</code>)
     */
    public static String trim(String str) {
        return (str == null ? null : str.trim() );
    }


    /**
     * Deletes all whitespace from a String.
     * Whitespace is defined by Character.isWhitespace
     *
     * @param str  String target to delete whitespace from
     * @return the text without whitespace
     * @throws NullPointerException
     */
    public static String deleteWhitespace(String str) {
        StringBuffer buffer = new StringBuffer();
        int sz = str.length();
        for (int i=0; i<sz; i++) {
            if(!Character.isWhitespace(str.charAt(i) ) ) {
                buffer.append(str.charAt(i) );
            }
        }
        return buffer.toString();
    }

    /**
     * Checks if a String is non null and is not empty (length > 0).
     *
     * @param str  the string to check
     * @return true if the String is non-null, and not length zero
     */
    public static boolean isNotEmpty(String str) {
        return (str != null && str.length() > 0);
    }

    /**
     * Checks if a (trimmed) String is null or empty.
     *
     * @param str  the string to check
     * @return true if the String is null, or length zero once trimmed
     */
    public static boolean isEmpty(String str) {
        return (str == null || str.trim().length() == 0);
    }

    // Equals and IndexOf
    //--------------------------------------------------------------------------

    /**
     * Compares two Strings, returning true if they are equal.
     * Nulls are handled without exceptions. Two <code>null</code>
     * references are considered equal. Comparison is case sensitive.
     *
     * @see java.lang.String#equals(Object)
     * @param str1  the first string
     * @param str2  the second string
     * @return true if the Strings are equal, case sensitive, or both null
     */
    public static boolean equals(String str1, String str2) {
        return (str1 == null ? str2 == null : str1.equals(str2) );
    }

    /**
     * Compares two Strings, returning true if they are equal ignoring case.
     * Nulls are handled without exceptions. Two <code>null</code>
     * references are considered equal. Comparison is case insensitive.
     * @see java.lang.String#equalsIgnoreCase(String)
     * @param str1  the first string
     * @param str2  the second string
     * @return true if the Strings are equal, case insensitive, or both null
     */
    public static boolean equalsIgnoreCase(String str1, String str2) {
        return (str1 == null ? str2 == null : str1.equalsIgnoreCase(str2) );
    }

    /**
     * Find the earliest index of any of a set of potential substrings.
     * Null string will return -1.
     * 
     * @param str  the string to check
     * @param searchStrs  the strings to search for
     * @return the earliest index of any of the strings
     * @throws NullPointerException if any of searchStrs[i] is null
     */
    public static int indexOfAny(String str, String[] searchStrs) {
        if ( (str == null) || (searchStrs == null) ) {
            return -1;
        }
        int sz = searchStrs.length;

        // String's can't have a MAX_VALUEth index.
        int ret = Integer.MAX_VALUE;

        int tmp = 0;
        for (int i = 0; i < sz; i++) {
            tmp = str.indexOf(searchStrs[i]);
            if (tmp == -1) {
                continue;
            }

            if (tmp < ret) {
                ret = tmp;
            }
        }

        return (ret == Integer.MAX_VALUE) ? -1 : ret;
    }

    /**
     * Find the latest index of any of a set of potential substrings.
     * Null string will return -1.
     * 
     * @param str  the string to check
     * @param searchStrs  the strings to search for
     * @return the last index of any of the strings
     * @throws NullPointerException if any of searchStrs[i] is null
     */
    public static int lastIndexOfAny(String str, String[] searchStrs) {
        if ( (str == null) || (searchStrs == null) ) {
            return -1;
        }
        int sz = searchStrs.length;
        int ret = -1;
        int tmp = 0;
        for (int i = 0; i < sz; i++) {
            tmp = str.lastIndexOf(searchStrs[i]);
            if (tmp > ret) {
                ret = tmp;
            }
        }
        return ret;
    }

    // Substring
    //--------------------------------------------------------------------------
    
    /**
     * Gets a substring of the specified string avoiding exceptions.
     * A negative start position can be used to start n characters from
     * the end of the string.
     * 
     * @param str  the string to get the substring from
     * @param start  the position to start from,  negative means 
     * count back from the end of the string by this many characters
     * @return substring from start position
     */
    public static String substring(String str, int start) {
        if (str == null) {
            return null;
        }

        // handle negatives, which means last n characters
        if (start < 0) {
            start = str.length() + start; // remember start is negative
        }

        if (start < 0) {
            start = 0;
        }
        if (start > str.length() ) {
            return "";
        }

        return str.substring(start);
    }
    
    /**
     * Gets a substring of the specified string avoiding exceptions.
     * A negative start position can be used to start/end n characters
     * from the end of the string.
     * 
     * @param str  the string to get the substring from
     * @param start  the position to start from, negative means 
     * count back from the end of the string by this many characters
     * @param end  the position to end at (exclusive),  negative means 
     * count back from the end of the string by this many characters
     * @return substring from start position to end positon
     */
    public static String substring(String str, int start, int end) {
        if (str == null) {
            return null;
        }

        // handle negatives
        if (end < 0) {
            end = str.length() + end; // remember end is negative
        }
        if (start < 0) {
            start = str.length() + start; // remember start is negative
        }

        // check length next
        if (end > str.length() ) {
            // check this works.
            end = str.length();
        }

        // if start is greater than end, return ""
        if (start > end) {
            return "";
        }

        if (start < 0) {
            start = 0;
        }
        if (end < 0) {
            end = 0;
        }

        return str.substring(start, end);
    }

    /**
     * Gets the leftmost n characters of a string. If n characters are not 
     * available, or the string is null, the string will be returned 
     * without an exception.
     *
     * @param str  the string to get the leftmost characters from
     * @param len  the length of the required string
     * @return the leftmost characters
     * @throws IllegalArgumentException if len is less than zero
     */
    public static String left(String str, int len) {
        if (len < 0) {
            throw new IllegalArgumentException("Requested String length " + len + " is less than zero");
        }
        if ( (str == null) || (str.length() <= len) ) {
            return str;
        } 
        else {
            return str.substring(0, len);
        }
    }

    /**
     * Gets the rightmost n characters of a string. If n characters are not 
     * available, or the string is null, the string will be returned 
     * without an exception.
     *
     * @param str  the string to get the rightmost characters from
     * @param len  the length of the required string
     * @return the leftmost characters
     * @throws IllegalArgumentException if len is less than zero
     */
    public static String right(String str, int len) {
        if (len < 0) {
            throw new IllegalArgumentException("Requested String length " + len + " is less than zero");
        }
        if ( (str == null) || (str.length() <= len) ) {
            return str;
        } 
        else {
            return str.substring(str.length() - len);
        }
    }

    /**
     * Gets n characters from the middle of a string. If n characters are 
     * not available, the remainder of the string will be returned 
     * without an exception. If the string is null, null will be returned.
     *
     * @param str  the string to get the characters from
     * @param pos  the position to start from
     * @param len  the length of the required string
     * @return the leftmost characters
     * @throws IndexOutOfBoundsException if pos is out of bounds
     * @throws IllegalArgumentException if len is less than zero
     */
    public static String mid(String str, int pos, int len) {
        if ( (pos < 0) || (str != null && pos > str.length() ) ) {
            throw new StringIndexOutOfBoundsException("String index " + pos + " is out of bounds");
        }
        if (len < 0) {
            throw new IllegalArgumentException("Requested String length " + len + " is less than zero");
        }
        if (str == null) {
            return null;
        }
        if (str.length() <= (pos + len) ) {
            return str.substring(pos);
        } 
        else {
            return str.substring(pos, pos + len);
        }
    }

    // Splitting
    //--------------------------------------------------------------------------
    
    /**
     * Splits the provided text into a list, using whitespace as the separator.
     * The separator is not included in the returned String array.
     *
     * @param str  the string to parse
     * @return an array of parsed Strings 
     */
    public static String[] split(String str) {
        return split(str, null, -1);
    }

    /**
     * @see #split(String, String, int)
     */
    public static String[] split(String text, String separator) {
        return split(text, separator, -1);
    }

    /**
     * Splits the provided text into a list, based on a given separator.
     * The separator is not included in the returned String array.
     * The maximum number of splits to perfom can be controlled.
     * A null separator will cause parsing to be on whitespace.
     *
     * <p>This is useful for quickly splitting a string directly into
     * an array of tokens, instead of an enumeration of tokens (as
     * <code>StringTokenizer</code> does).
     *
     * @param str The string to parse.
     * @param separator Characters used as the delimiters. If
     * <code>null</code>, splits on whitespace.
     * @param max The maximum number of elements to include in the
     * list.  A zero or negative value implies no limit.
     * @return an array of parsed Strings 
     */
    public static String[] split(String str, String separator, int max) {
        StringTokenizer tok = null;
        if (separator == null) {
            // Null separator means we're using StringTokenizer's default
            // delimiter, which comprises all whitespace characters.
            tok = new StringTokenizer(str);
        } 
        else {
            tok = new StringTokenizer(str, separator);
        }

        int listSize = tok.countTokens();
        if (max > 0 && listSize > max) {
            listSize = max;
        }

        String[] list = new String[listSize];
        int i = 0;
       int lastTokenBegin = 0;
        int lastTokenEnd = 0;
        while (tok.hasMoreTokens() ) {
            if (max > 0 && i == listSize - 1) {
                // In the situation where we hit the max yet have
                // tokens left over in our input, the last list
                // element gets all remaining text.
                String endToken = tok.nextToken();
                lastTokenBegin = str.indexOf(endToken, lastTokenEnd);
                list[i] = str.substring(lastTokenBegin);
                break;
            } 
            else {
                list[i] = tok.nextToken();
                lastTokenBegin = str.indexOf(list[i], lastTokenEnd);
                lastTokenEnd = lastTokenBegin + list[i].length();
            }
            i++;
        }  
        return list;
    }

    // Joining
    //--------------------------------------------------------------------------
    /**
     * Concatenates elements of an array into a single string.
     * The difference from join is that concatenate has no delimiter.
     * 
     * @param array  the array of values to concatenate.
     * @return the concatenated string.
     */
    public static String concatenate(Object[] array) {
        return join(array, "");
    }
    
    /**
     * Joins the elements of the provided array into a single string
     * containing the provided list of elements. 
     * No delimiter is added before or after the list.
     * A null separator is the same as a blank String.
     *
     * @param array  the array of values to join together
     * @param separator  the separator character to use
     * @return the joined String
     */
    public static String join(Object[] array, String separator) {
        if (separator == null) {
            separator = "";
        }
        int arraySize = array.length;
        int bufSize = (arraySize == 0 ? 0 : (array[0].toString().length() +
                                 separator.length() ) * arraySize);
        StringBuffer buf = new StringBuffer(bufSize);

        for (int i = 0; i < arraySize; i++) {
            if (i > 0) {
                buf.append(separator);
            }
            buf.append(array[i]);
        }
        return buf.toString();
    }

    /**
     * Joins the elements of the provided iterator into a single string
     * containing the provided elements.
     * No delimiter is added before or after the list.
     * A null separator is the same as a blank String.
     *
     * @param iterator  the iterator of values to join together
     * @param separator  the separator character to use
     * @return the joined String
     */
    public static String join(Iterator<?> iterator, String separator) {
        if (separator == null) {
            separator = "";
        }
        StringBuffer buf = new StringBuffer(256);  // Java default is 16, probably too small
        while (iterator.hasNext() ) {
            buf.append(iterator.next() );
            if (iterator.hasNext() ) {
                buf.append(separator);
            }
        }
        return buf.toString();
    }



    // Replacing
    //--------------------------------------------------------------------------
    
    /**
     * Replace a string with another string inside a larger string, once.
     *
     * @see #replace(String text, String repl, String with, int max)
     * @param text  text to search and replace in
     * @param repl  String to search for
     * @param with  String to replace with
     * @return the text with any replacements processed
     */
    public static String replaceOnce(String text, String repl, String with) {
        return replace(text, repl, with, 1);
    }

    /**
     * Replace all occurances of a string within another string.
     *
     * @see #replace(String text, String repl, String with, int max)
     * @param text  text to search and replace in
     * @param repl  String to search for
     * @param with  String to replace with
     * @return the text with any replacements processed
     */
    public static String replace(String text, String repl, String with) {
        return replace(text, repl, with, -1);
    }

    /**
     * Replace a string with another string inside a larger string,
     * for the first <code>max</code> values of the search string.  A
     * <code>null</code> reference is passed to this method is a
     * no-op.
     *
     * @param text  text to search and replace in
     * @param repl  String to search for
     * @param with  String to replace with
     * @param max  maximum number of values to replace, or
     * <code>-1</code> if no maximum
     * @return the text with any replacements processed
     * @throws NullPointerException if repl is null
     */
    public static String replace(String text, String repl, String with,
                                 int max) {
        if (text == null) {
            return null;
        }

        StringBuffer buf = new StringBuffer(text.length() );
        int start = 0, end = 0;
        while ( (end = text.indexOf(repl, start) ) != -1) {
            buf.append(text.substring(start, end) ).append(with);
            start = end + repl.length();

            if (--max == 0) {
                break;
            }
        }
        buf.append(text.substring(start) );
        return buf.toString();
    }

    /**
     * Overlay a part of a string with another string.
     *
     * @param text String to do overlaying in
     * @param overlay String to overlay
     * @param start int to start overlaying at
     * @param end   int to stop overlaying before
     * @return String with overlayed text
     * @throws NullPointerException if text or overlay is null
     */
    public static String overlayString(String text, String overlay, int start, int end) {
        return new StringBuffer(start + overlay.length() + text.length() - end + 1)
            .append(text.substring(0, start) )
            .append(overlay)
            .append(text.substring(end) )
            .toString();
    }

    // Centering
    //--------------------------------------------------------------------------
    
    /**
     * Center a string in a larger string of size n.
     * Uses spaces as the value to buffer the string with.
     * Equivalent to <code>center(str, size, "")</code>
     *
     * @param str  String to center
     * @param size  int size of new String
     * @return String containing centered String
     * @throws NullPointerException if str is null
     */
    public static String center(String str, int size) {
        return center(str, size, " ");
    }

    /**
     * Center a string in a larger string of size n.
     * Uses a supplied String as the value to buffer the string with..
     *
     * @param str  String to center
     * @param size  int size of new String
     * @param delim  String to buffer the new String with
     * @return String containing centered String
     * @throws NullPointerException if str or delim is null
     * @throws ArithmeticException if delim is the empty string
     */
    public static String center(String str, int size, String delim) {
        int sz = str.length();
        int p = size - sz;
        if (p < 1) {
            return str;
        }
        str = leftPad(str, sz + p / 2, delim);
        str = rightPad(str, size, delim);
        return str;
    }

    // Chomping
    //--------------------------------------------------------------------------
    
    /** 
     * Remove the last newline, and everything after it from a String.
     *
     * @param str  String to chomp the newline from
     * @return String without chomped newline
     * @throws NullPointerException if str is null
     */
    public static String chomp(String str) {
        return chomp(str, "\n");
    }
    
    /** 
     * Remove the last value of a supplied String, and everything after it 
     * from a String.
     *
     * @param str  String to chomp from
     * @param sep  String to chomp
     * @return String without chomped ending
     * @throws NullPointerException if str or sep is null
     */
    public static String chomp(String str, String sep) {
        int idx = str.lastIndexOf(sep);
        if (idx != -1) {
            return str.substring(0, idx);
        } 
        else {
            return str;
        }
    }
    
    /**
     * Remove a newline if and only if it is at the end 
     * of the supplied string.
     * 
     * @param str  String to chomp from
     * @return String without chomped ending
     * @throws NullPointerException if str is null
     */
    public static String chompLast(String str) {
        return chompLast(str, "\n");
    }
    
    /**
     * Remove a value if and only if the string ends with that value.
     * 
     * @param str  String to chomp from
     * @param sep  String to chomp
     * @return String without chomped ending
     * @throws NullPointerException if str or sep is null
     */
    public static String chompLast(String str, String sep) {
        if (str.length() == 0) {
            return str;
        }
        String sub = str.substring(str.length() - sep.length() );
        if (sep.equals(sub) ) {
            return str.substring(0, str.length() - sep.length() );
        } 
        else {
            return str;
        }
    }

    /** 
     * Remove everything and return the last value of a supplied String, and 
     * everything after it from a String.
     *
     * @param str String to chomp from
     * @param sep String to chomp
     * @return String chomped
     * @throws NullPointerException if str or sep is null
     */
    public static String getChomp(String str, String sep) {
        int idx = str.lastIndexOf(sep);
        if (idx == str.length() - sep.length() ) {
            return sep;
        } 
        else if (idx != -1) {
            return str.substring(idx);
        } 
        else {
            return "";
        }
    }

    /** 
     * Remove the first value of a supplied String, and everything before it 
     * from a String.
     *
     * @param str String to chomp from
     * @param sep String to chomp
     * @return String without chomped beginning
     * @throws NullPointerException if str or sep is null
     */
    public static String prechomp(String str, String sep) {
        int idx = str.indexOf(sep);
        if (idx != -1) {
            return str.substring(idx + sep.length() );
        } 
        else {
            return str;
        }
    }

    /** 
     * Remove and return everything before the first value of a 
     * supplied String from another String.
     *
     * @param str String to chomp from
     * @param sep String to chomp
     * @return String prechomped
     * @throws NullPointerException if str or sep is null
     */
    public static String getPrechomp(String str, String sep) {
        int idx = str.indexOf(sep);
        if (idx != -1) {
            return str.substring(0, idx + sep.length() );
        } 
        else {
            return "";
        }
    }

    // Chopping
    //--------------------------------------------------------------------------
    
    /**
     * Remove the last character from a String. If the String 
     * ends in \r\n, then remove both of them.
     *
     * @param str String to chop last character from
     * @return String without last character
     * @throws NullPointerException if str is null
     */
    public static String chop(String str) {
        if ("".equals(str) ) {
            return "";
        }
        if (str.length() == 1) {
            return "";
        }
        int lastIdx = str.length() - 1;
        String ret = str.substring(0, lastIdx);
        char last = str.charAt(lastIdx);
        if (last == '\n') {
            if (ret.charAt(lastIdx - 1) == '\r') {
                return ret.substring(0, lastIdx - 1);
            }
        }
        return ret;
    }

    /**
     * Remove \n from end of a String if it's there.
     * If a \r precedes it, then remove that too.
     *
     * @param str String to chop a newline from
     * @return String without newline
     * @throws NullPointerException if str is null
     */
    public static String chopNewline(String str) {
        int lastIdx = str.length() - 1;
        char last = str.charAt(lastIdx);
        if (last == '\n') {
            if (str.charAt(lastIdx - 1) == '\r') {
                lastIdx--;
            }
        }
        else {
            lastIdx++;
        }
        return str.substring(0, lastIdx);
    }


    // Conversion
    //--------------------------------------------------------------------------
    
    // spec 3.10.6
    /**
     * Escapes any values it finds into their String form.
     * So a tab becomes the characters '\\' and 't'.
     *
     * @param str String to escape values in
     *
     * @return String with escaped values
     * @throws NullPointerException if str is null
     */
    public static String escape(String str) {
        // improved with code from  cybertiger@cyberiantiger.org
        // unicode from him, and defaul for < 32's.
        int sz = str.length();
        StringBuffer buffer = new StringBuffer(2 * sz);
        for (int i = 0; i < sz; i++) {
            char ch = str.charAt(i);

            // handle unicode
            if (ch > 0xfff) {
                buffer.append("\\u" + Integer.toHexString(ch) );
            } 
            else if (ch > 0xff) {
                buffer.append("\\u0" + Integer.toHexString(ch) );
            } 
            else if (ch > 0x7f) {
                buffer.append("\\u00" + Integer.toHexString(ch) );
            } 
            else if (ch < 32) {
                switch (ch) {
                    case '\b' :
                        buffer.append('\\');
                        buffer.append('b');
                        break;
                    case '\n' :
                        buffer.append('\\');
                        buffer.append('n');
                        break;
                    case '\t' :
                        buffer.append('\\');
                        buffer.append('t');
                        break;
                    case '\f' :
                        buffer.append('\\');
                        buffer.append('f');
                        break;
                    case '\r' :
                        buffer.append('\\');
                        buffer.append('r');
                        break;
                    default :
                        if (ch > 0xf) {
                            buffer.append("\\u00" + Integer.toHexString(ch) );
                        } 
                        else {
                            buffer.append("\\u000" + Integer.toHexString(ch) );
                        }
                        break;
                }
            } 
            else {
                switch (ch) {
                    case '\'' :
                        buffer.append('\\');
                        buffer.append('\'');
                        break;
                    case '"' :
                        buffer.append('\\');
                        buffer.append('"');
                        break;
                    case '\\' :
                        buffer.append('\\');
                        buffer.append('\\');
                        break;
                    default :
                        buffer.append(ch);
                        break;
                }
            }
        }
        return buffer.toString();
    }

    // Padding
    //--------------------------------------------------------------------------
    
    /**
     * Repeat a string n times to form a new string.
     *
     * @param str  String to repeat
     * @param repeat  int number of times to repeat
     * @return String with repeated string
     * @throws NegativeArraySizeException if repeat < 0
     * @throws NullPointerException if str is null
     */
    public static String repeat(String str, int repeat) {
        StringBuffer buffer = new StringBuffer(repeat * str.length() );
        for (int i = 0; i < repeat; i++) {
            buffer.append(str);
        }
        return buffer.toString();
    }

    /**
     * Right pad a String with spaces. Pad to a size of n.
     * 
     * @param str  String to repeat
     * @param size  int number of times to repeat
     * @return right padded String
     * @throws NullPointerException if str is null
     */
    public static String rightPad(String str, int size) {
        return rightPad(str, size, " ");
    }
    
    /**
     * Right pad a String with a specified string. Pad to a size of n.
     *
     * @param str  String to pad out
     * @param size  int size to pad to
     * @param delim  String to pad with
     * @return right padded String
     * @throws NullPointerException if str or delim is null
     * @throws ArithmeticException if delim is the empty string
     */
    public static String rightPad(String str, int size, String delim) {
        size = (size - str.length() ) / delim.length();
        if (size > 0) {
            str += repeat(delim, size);
        }
        return str;
    }

    /**
     * Left pad a String with spaces. Pad to a size of n.
     *
     * @param str  String to pad out
     * @param size  int size to pad to
     * @return left padded String
     * @throws NullPointerException if str or delim is null
     */
    public static String leftPad(String str, int size) {
        return leftPad(str, size, " ");
    }
    /**
     * Left pad a String with a specified string. Pad to a size of n.
     *
     * @param str  String to pad out
     * @param size  int size to pad to
     * @param delim  String to pad with
     * @return left padded String
     * @throws NullPointerException if str or delim is null
     * @throws ArithmeticException if delim is the empty string
     */
    public static String leftPad(String str, int size, String delim) {
        size = (size - str.length() ) / delim.length();
        if (size > 0) {
            str = repeat(delim, size) + str;
        }
        return str;
    }

    // Stripping
    //--------------------------------------------------------------------------
    
    /**
     * Remove whitespace from the front and back of a String.
     * 
     * @param str  the string to remove whitespace from
     * @return the stripped string
     */
    public static String strip(String str) {
        return strip(str, null);
    }
    /**
     * Remove a specified String from the front and back of a 
     * String. If Whitespace is wanted to be removed, used the 
     * strip(String) method.
     * 
     * @param str  the string to remove a string from
     * @param delim  the string to remove at start and end
     * @return the stripped string
     */
    public static String strip(String str, String delim) {
        str = stripStart(str, delim);
        return stripEnd(str, delim);
    }

    /**
     * Strip whitespace from the front and back of every string
     * in the array.
     * 
     * @param strs  the strings to remove whitespace from
     * @return the stripped strings
     */
    public static String[] stripAll(String[] strs) {
        return stripAll(strs, null);
    }
 
    /**
     * Strip the specified delimiter from the front and back of
     * every String in the array.
     * 
     * @param strs  the strings to remove a string from
     * @param delimiter  the string to remove at start and end
     * @return the stripped strings
     */
    public static String[] stripAll(String[] strs, String delimiter) {
        if ( (strs == null) || (strs.length == 0) ) {
            return strs;
        }
        int sz = strs.length;
        String[] newArr = new String[sz];
        for (int i = 0; i < sz; i++) {
            newArr[i] = strip(strs[i], delimiter);
        }
        return newArr;
    }   

    /**
     * Strip any of a supplied string from the end of a String..
     * If the strip string is null, whitespace is stripped.
     * 
     * @param str  the string to remove characters from
     * @param strip  the string to remove
     * @return the stripped string
     */
    public static String stripEnd(String str, String strip) {
        if (str == null) {
            return null;
        }
        int end = str.length();
 
        if (strip == null) {
            while ( (end != 0) && Character.isWhitespace(str.charAt(end - 1) ) ) {
                end--;
            }
        } 
        else {
            while ( (end != 0) && (strip.indexOf(str.charAt(end - 1) ) != -1) ) {
                end--;
            }
        }
        return str.substring(0, end);
    }

    /**
     * Strip any of a supplied string from the start of a String.
     * If the strip string is null, whitespace is stripped.
     * 
     * @param str  the string to remove characters from
     * @param strip  the string to remove
     * @return the stripped string
     */
    public static String stripStart(String str, String strip) {
        if (str == null) {
            return null;
        }
 
        int start = 0;
 
        int sz = str.length();
 
        if (strip == null) {
            while ( (start != sz) && Character.isWhitespace(str.charAt(start) ) ) {
                start++;
            }
        } 
        else {
            while ( (start != sz) && (strip.indexOf(str.charAt(start) ) != -1) ) {
                start++;
            }
        }
        return str.substring(start);
    }

    // Case conversion
    //--------------------------------------------------------------------------
    
    /**
     * Convert a String to upper case, null string returns null.
     * 
     * @param str  the string to uppercase
     * @return the upper cased string
     */
    public static String upperCase(String str) {
        if (str == null) {
            return null;
        }
        return str.toUpperCase();
    }

    /**
     * Convert a String to lower case, null string returns null.
     * 
     * @param str  the string to lowercase
     * @return the lower cased string
     */
    public static String lowerCase(String str) {
        if (str == null) {
            return null;
        }
        return str.toLowerCase();
    }

    /**
     * Uncapitalise a string. That is, convert the first character into 
     * lower-case. Null is returned as null.
     *
     * @param str  the string to uncapitalise
     * @return uncapitalised string
     */
    public static String uncapitalise(String str) {
        if (str == null) {
            return null;
        }
        if (str.length() == 0) {
            return "";
        }
        return new StringBuffer(str.length() )
            .append(Character.toLowerCase(str.charAt(0) ) )
            .append(str.substring(1) )
            .toString();
    }

    /**
     * Capitalise a string. That is, convert the first character into 
     * title-case. Null is returned as null.
     *
     * @param str  the string to capitalise
     * @return capitalised string
     */
    public static String capitalise(String str) {
        if (str == null) {
            return null;
        }
        if (str.length() == 0) {
            return "";
        }
        return new StringBuffer(str.length() )
            .append(Character.toTitleCase(str.charAt(0) ) )
            .append(str.substring(1) )
            .toString();
    }

    /**
     * Swaps the case of String. Properly looks after 
     * making sure the start of words are Titlecase and not 
     * Uppercase. Null is returned as null.
     * 
     * @param str  the string to swap the case of
     * @return the modified string
     */
    public static String swapCase(String str) {
        if (str == null) {
            return null;
        }
        int sz = str.length();
        StringBuffer buffer = new StringBuffer(sz);

        boolean whitespace = false;
        char ch = 0;
        char tmp = 0;

        for (int i = 0; i < sz; i++) {
            ch = str.charAt(i);
            if (Character.isUpperCase(ch) ) {
                tmp = Character.toLowerCase(ch);
            } 
            else if (Character.isTitleCase(ch) ) {
                tmp = Character.toLowerCase(ch);
            } 
            else if (Character.isLowerCase(ch) ) {
                if (whitespace) {
                    tmp = Character.toTitleCase(ch);
                } 
                else {
                    tmp = Character.toUpperCase(ch);
                }
            } 
            else {
                tmp = ch;
            }
            buffer.append(tmp);
            whitespace = Character.isWhitespace(ch);
        }
        return buffer.toString();
    }


    /**
     * Capitalise all the words in a string. Uses Character.isWhitespace 
     * as a separator between words. Null will return null.
     *
     * @param str  the string to capitalise
     * @return capitalised string
     */
    public static String capitaliseAllWords(String str) {
        if (str == null) {
            return null;
        }
        int sz = str.length();
        StringBuffer buffer = new StringBuffer(sz);
        boolean space = true;
        for (int i = 0; i < sz; i++) {
            char ch = str.charAt(i);
            if (Character.isWhitespace(ch) ) {
                buffer.append(ch);
                space = true;
            } 
            else if (space) {
                buffer.append(Character.toTitleCase(ch) );
                space = false;
            } 
            else {
                buffer.append(ch);
            }
        }
        return buffer.toString();
    }

    // Nested extraction
    //--------------------------------------------------------------------------
    
    /**
     * Get the String that is nested in between two instances of the 
     * same String. If str is null, will return null
     *
     * @param str  the string containing nested-string
     * @param tag  the string before and after nested-string
     * @return the string that was nested, or null
     * @throws NullPointerException if tag is null
     */
    public static String getNestedString(String str, String tag) {
        return getNestedString(str, tag, tag);
    }
    
    /**
     * Get the string that is nested in between two strings.
     *
     * @param str  the string containing nested-string
     * @param open  the string before nested-string
     * @param close  the string after nested-string
     * @return the string that was nested, or null
     * @throws NullPointerException if open or close  is null
     */
    public static String getNestedString(String str, String open, String close) {
        if (str == null) {
            return null;
        }
        int start = str.indexOf(open);
        if (start != -1) {
            int end = str.indexOf(close, start + open.length() );
            if (end != -1) {
                return str.substring(start + open.length(), end);
            }
        }
        return null;
    }

    /**
     * How many times is the substring in the larger string.
     * Null returns 0.
     * 
     * @param str  the string to check
     * @param sub  the substring to count
     * @return the number of occurances, 0 if the string is null
     * @throws NullPointerException if sub is null
     */
    public static int countMatches(String str, String sub) {
        if (str == null) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while ( (idx = str.indexOf(sub, idx) ) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    // Character Tests
    //--------------------------------------------------------------------------
    
    /**
     * Checks if the string contains only unicode letters.
     * Null will return false. The empty string will return true.
     * 
     * @param str  the string to check
     * @return true if only contains letters, and is non-null
     */
    public static boolean isAlpha(String str) {
        if (str == null) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isLetter(str.charAt(i) ) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the string contains only unicode letters and space (' ').
     * Null will return false.  The empty string will return true.
     * 
     * @param str  the string to check
     * @return true if only contains letters and space, and is non-null
     */
    public static boolean isAlphaSpace(String str) {
        if (str == null) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if ( (!Character.isLetter(str.charAt(i) ) ) &&
                (str.charAt(i) != ' ') ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the string contains only unicode letters or digits.
     * Null will return false. The empty string will return true.
     * 
     * @param str  the string to check
     * @return true if only contains letters or digits, and is non-null
     */
    public static boolean isAlphanumeric(String str) {
        if (str == null) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isLetterOrDigit(str.charAt(i) ) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the string contains only unicode letters, digits or space (' ').
     * Null will return false. The empty string will return true.
     * 
     * @param str  the string to check
     * @return true if only contains letters, digits or space, and is non-null
     */
    public static boolean isAlphanumericSpace(String str) {
        if (str == null) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if ( (!Character.isLetterOrDigit(str.charAt(i) ) ) &&
                (str.charAt(i) != ' ') ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the string contains only unicode digits.
     * Null will return false. The empty string will return true.
     * 
     * @param str  the string to check
     * @return true if only contains digits, and is non-null
     */
    public static boolean isNumeric(String str) {
        if (str == null) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isDigit(str.charAt(i) ) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the string contains only unicode digits or space (' ').
     * Null will return false. The empty string will return true.
     * 
     * @param str  the string to check
     * @return true if only contains digits or space, and is non-null
     */
    public static boolean isNumericSpace(String str) {
        if (str == null) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if ( (!Character.isDigit(str.charAt(i) ) ) &&
                (str.charAt(i) != ' ') ) {
                return false;
            }
        }
        return true;
    }

    // Defaults
    //--------------------------------------------------------------------------
    
    /**
     * Return either the passed in String, or if it is null, 
     * then an empty String.
     * 
     * @param str  the string to check
     * @return the passed in string, or blank if it was null
     */
    public static String defaultString(String str) {
        return defaultString(str, "");
    }

    /**
     * Return either the passed in String, or if it is null, 
     * then a passed in default String.
     * 
     * @param str  the string to check
     * @param defaultString  the default string to return is str is null
     * @return the passed in string, or the default if it was null
     */
    public static String defaultString(String str, String defaultString) {
        return (str == null) ? defaultString : str;
    }

    // Reversing
    //--------------------------------------------------------------------------

    /**
     * Reverse a String, null string returns null.
     * 
     * @param str  the string to reverse
     * @return the reversed string
     */
    public static String reverse(String str) {
        if (str == null) {
            return null;
        }
        return new StringBuffer(str).reverse().toString();
    }

    /**
     * Reverses a string that is delimited by a specific character.
     * The strings between the delimiters are not reversed.
     * Thus java.lang.String becomes String.lang.java (if the delimiter is '.').
     * 
     * @param str  the string to reverse
     * @param delimiter  the delimiter to use
     * @return the reversed string
     */
    public static String reverseDelimitedString(String str, String delimiter) {
        // could implement manually, but simple way is to reuse other, 
        // probably slower, methods.
        String[] strs = split(str, delimiter);
        reverseArray(strs);
        return join(strs, delimiter);
    }

    /**
     * Reverses an array. 
     * TAKEN FROM CollectionsUtils.
     * @param array  the array to reverse
     */
    private static void reverseArray(Object[] array) {
        int i = 0;
        int j = array.length - 1;
        Object tmp;

        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }


    // Misc
    //--------------------------------------------------------------------------

    /**
     * Checks if the String contains only certain chars.
     *
     * @param str the String to check
     * @param valid an array of valid chars
     * @return true if it only contains valid chars and is non-null
     */
    public static boolean containsOnly(String str, char[] valid) {
        if (str == null || valid == null) {
            return false;
        }

        int strSize = str.length();
        int validSize = valid.length;

        for (int i = 0; i < strSize; i++) {
            boolean contains = false;
            for (int j = 0; j < validSize; j++) {
                if (valid[j] == str.charAt(i) ) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                return false;
            }
        }

        return true;
    }
    
    /**
     * Strip any of the supplied string from the start of a string but
     * do not strip more than maxStrip characters.
     * <p>
     * If maxStrip value is zero then no restriction is imposed.
     * <p>
     * @param str  the string to remove characters from
     * @param strip  the string to remove
     * @param max characters to strip
     * @return the stripped string
     */
    public static String stripStart(String str, String strip, int maxStrip) {
    	if (str == null) return null;

    	if (maxStrip > 0 && str.length() > maxStrip)
    		return stripStart(str.substring(0,maxStrip), strip) + str.substring(maxStrip);
    	else
    		return stripStart(str, strip);
    }
}
