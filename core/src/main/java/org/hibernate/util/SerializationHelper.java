/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
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
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
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
package org.hibernate.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.ObjectStreamClass;
import java.io.ObjectInputStream;

import org.hibernate.type.SerializationException;
import org.hibernate.Hibernate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>Assists with the serialization process and performs additional functionality based
 * on serialization.</p>
 * <p>
 * <ul>
 * <li>Deep clone using serialization
 * <li>Serialize managing finally and IOException
 * <li>Deserialize managing finally and IOException
 * </ul>
 *
 * <p>This class throws exceptions for invalid <code>null</code> inputs.
 * Each method documents its behaviour in more detail.</p>
 *
 * @author <a href="mailto:nissim@nksystems.com">Nissim Karpenstein</a>
 * @author <a href="mailto:janekdb@yahoo.co.uk">Janek Bogucki</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @author Stephen Colebourne
 * @author Jeff Varszegi
 * @author Gary Gregory
 * @since 1.0
 * @version $Id: SerializationHelper.java 9180 2006-01-30 23:51:27Z steveebersole $
 */
public final class SerializationHelper {

	private static final Log log = LogFactory.getLog(SerializationHelper.class);

    private SerializationHelper() {}

    // Clone
    //-----------------------------------------------------------------------
    /**
     * <p>Deep clone an <code>Object</code> using serialization.</p>
     *
     * <p>This is many times slower than writing clone methods by hand
     * on all objects in your object graph. However, for complex object
     * graphs, or for those that don't support deep cloning this can
     * be a simple alternative implementation. Of course all the objects
     * must be <code>Serializable</code>.</p>
     *
     * @param object  the <code>Serializable</code> object to clone
     * @return the cloned object
     * @throws SerializationException (runtime) if the serialization fails
     */
    public static Object clone(Serializable object) throws SerializationException {
	    log.trace("Starting clone through serialization");
        return deserialize( serialize(object) );
    }

    // Serialize
    //-----------------------------------------------------------------------
    /**
     * <p>Serializes an <code>Object</code> to the specified stream.</p>
     *
     * <p>The stream will be closed once the object is written.
     * This avoids the need for a finally clause, and maybe also exception
     * handling, in the application code.</p>
     *
     * <p>The stream passed in is not buffered internally within this method.
     * This is the responsibility of your application if desired.</p>
     *
     * @param obj  the object to serialize to bytes, may be null
     * @param outputStream  the stream to write to, must not be null
     * @throws IllegalArgumentException if <code>outputStream</code> is <code>null</code>
     * @throws SerializationException (runtime) if the serialization fails
     */
    public static void serialize(Serializable obj, OutputStream outputStream) throws SerializationException {
        if (outputStream == null) {
            throw new IllegalArgumentException("The OutputStream must not be null");
        }

	    if ( log.isTraceEnabled() ) {
		    if ( Hibernate.isInitialized( obj ) ) {
	            log.trace( "Starting serialization of object [" + obj + "]" );
		    }
		    else {
			    log.trace( "Starting serialization of [uninitialized proxy]" );
		    }
	    }

        ObjectOutputStream out = null;
        try {
            // stream closed in the finally
            out = new ObjectOutputStream(outputStream);
            out.writeObject(obj);

        }
        catch (IOException ex) {
            throw new SerializationException("could not serialize", ex);
        }
        finally {
            try {
                if (out != null) out.close();
            }
            catch (IOException ignored) {}
        }
    }

    /**
     * <p>Serializes an <code>Object</code> to a byte array for
     * storage/serialization.</p>
     *
     * @param obj  the object to serialize to bytes
     * @return a byte[] with the converted Serializable
     * @throws SerializationException (runtime) if the serialization fails
     */
    public static byte[] serialize(Serializable obj) throws SerializationException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
        serialize(obj, baos);
        return baos.toByteArray();
    }

    // Deserialize
    //-----------------------------------------------------------------------
    /**
     * <p>Deserializes an <code>Object</code> from the specified stream.</p>
     *
     * <p>The stream will be closed once the object is written. This
     * avoids the need for a finally clause, and maybe also exception
     * handling, in the application code.</p>
     *
     * <p>The stream passed in is not buffered internally within this method.
     * This is the responsibility of your application if desired.</p>
     *
     * @param inputStream  the serialized object input stream, must not be null
     * @return the deserialized object
     * @throws IllegalArgumentException if <code>inputStream</code> is <code>null</code>
     * @throws SerializationException (runtime) if the serialization fails
     */
    public static Object deserialize(InputStream inputStream) throws SerializationException {
        if (inputStream == null) {
            throw new IllegalArgumentException("The InputStream must not be null");
        }

		log.trace("Starting deserialization of object");

        CustomObjectInputStream in = null;
        try {
            // stream closed in the finally
            in = new CustomObjectInputStream(inputStream);
            return in.readObject();

        }
        catch (ClassNotFoundException ex) {
            throw new SerializationException("could not deserialize", ex);
        }
        catch (IOException ex) {
            throw new SerializationException("could not deserialize", ex);
        }
        finally {
            try {
                if (in != null) in.close();
            }
            catch (IOException ex) {}
        }
    }

    /**
     * <p>Deserializes a single <code>Object</code> from an array of bytes.</p>
     *
     * @param objectData  the serialized object, must not be null
     * @return the deserialized object
     * @throws IllegalArgumentException if <code>objectData</code> is <code>null</code>
     * @throws SerializationException (runtime) if the serialization fails
     */
    public static Object deserialize(byte[] objectData) throws SerializationException {
        if (objectData == null) {
            throw new IllegalArgumentException("The byte[] must not be null");
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(objectData);
        return deserialize(bais);
    }


	/**
	 * Custom ObjectInputStream implementation to more appropriately handle classloading
	 * within app servers (mainly jboss - hence this class inspired by jboss's class of
	 * the same purpose).
	 */
	private static final class CustomObjectInputStream extends ObjectInputStream {

		public CustomObjectInputStream(InputStream in) throws IOException {
			super(in);
		}

		protected Class resolveClass(ObjectStreamClass v) throws IOException, ClassNotFoundException {
			String className = v.getName();
			Class resolvedClass = null;

			log.trace("Attempting to locate class [" + className + "]");

			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			try {
				resolvedClass = loader.loadClass(className);
				log.trace("Class resolved through context class loader");
			}
			catch(ClassNotFoundException e) {
				log.trace("Asking super to resolve");
				resolvedClass = super.resolveClass(v);
			}

			return resolvedClass;
		}
	}
}
