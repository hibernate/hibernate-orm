/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.tools;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.NotFoundException;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.envers.internal.entities.PropertyData;

import static org.hibernate.envers.internal.tools.StringTools.capitalizeFirst;
import static org.hibernate.envers.internal.tools.StringTools.getLastComponent;

/**
 * @author Lukasz Zuchowski (author at zuchos dot com)
 */
public final class MapProxyTool {
	private MapProxyTool() {
	}

	/**
	 * @param className Name of the class to construct (should be unique within class loader)
	 * @param map instance that will be proxied by java bean
	 * @param propertyDatas properties that should java bean declare
	 * @param classLoaderService
	 *
	 * @return new instance of proxy
	 *
	 * @author Lukasz Zuchowski (author at zuchos dot com)
	 * Creates instance of map proxy class. This proxy class will be a java bean with properties from <code>propertyDatas</code>.
	 * Instance will proxy calls to instance of the map passed as parameter.
	 */
	public static Object newInstanceOfBeanProxyForMap(
			String className,
			Map<String, Object> map,
			Set<PropertyData> propertyDatas,
			ClassLoaderService classLoaderService) {
		Map<String, Class<?>> properties = prepareProperties( propertyDatas );
		return createNewInstance( map, classForName( className, properties, classLoaderService ) );
	}

	private static Object createNewInstance(Map<String, Object> map, Class aClass) {
		try {
			return aClass.getConstructor( Map.class ).newInstance( map );
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	private static Map<String, Class<?>> prepareProperties(Set<PropertyData> propertyDatas) {
		Map<String, Class<?>> properties = new HashMap<String, Class<?>>();
		for ( PropertyData propertyData : propertyDatas ) {
			properties.put( propertyData.getBeanName(), Object.class );
		}
		return properties;
	}

	private static Class loadClass(String className, ClassLoaderService classLoaderService) {
		try {
			return ReflectionTools.loadClass( className, classLoaderService );
		}
		catch (ClassLoadingException e) {
			return null;
		}

	}

	/**
	 * Generates/loads proxy class for given name with properties for map.
	 *
	 * @param className name of the class that will be generated/loaded
	 * @param properties list of properties that should be exposed via java bean
	 * @param classLoaderService
	 *
	 * @return proxy class that wraps map into java bean
	 */
	public static Class classForName(
			String className,
			Map<String, Class<?>> properties,
			ClassLoaderService classLoaderService) {
		Class aClass = loadClass( className, classLoaderService );
		if ( aClass == null ) {
			aClass = generate( className, properties );
		}
		return aClass;
	}

	/**
	 * Protected for test only
	 */
	protected static Class generate(String className, Map<String, Class<?>> properties) {
		try {
			ClassPool pool = ClassPool.getDefault();
			CtClass cc = pool.makeClass( className );

			cc.addInterface( resolveCtClass( Serializable.class ) );
			cc.addField( new CtField( resolveCtClass( Map.class ), "theMap", cc ) );
			cc.addConstructor( generateConstructor( className, cc ) );

			for ( Entry<String, Class<?>> entry : properties.entrySet() ) {

				// add getter
				cc.addMethod( generateGetter( cc, entry.getKey(), entry.getValue() ) );

				// add setter
				cc.addMethod( generateSetter( cc, entry.getKey(), entry.getValue() ) );
			}
			return cc.toClass();
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	private static CtConstructor generateConstructor(String className, CtClass cc)
			throws NotFoundException, CannotCompileException {
		StringBuffer sb = new StringBuffer();
		sb.append( "public " )
				.append( getLastComponent( className ) )
				.append( "(" )
				.append( Map.class.getName() )
				.append( " map)" )
				.append( "{" )
				.append( "this.theMap = map;" )
				.append( "}" );
		System.out.println( sb );
		return CtNewConstructor.make( sb.toString(), cc );
	}

	private static CtMethod generateGetter(CtClass declaringClass, String fieldName, Class fieldClass)
			throws CannotCompileException {

		String getterName = "get" + capitalizeFirst( fieldName );

		StringBuilder sb = new StringBuilder();
		sb.append( "public " ).append( fieldClass.getName() ).append( " " )
				.append( getterName ).append( "(){" ).append( "return (" ).append( fieldClass.getName() ).append(
				")this.theMap.get(\""
		)
				.append( fieldName ).append( "\")" ).append( ";" ).append( "}" );
		return CtMethod.make( sb.toString(), declaringClass );
	}

	private static CtMethod generateSetter(CtClass declaringClass, String fieldName, Class fieldClass)
			throws CannotCompileException {

		String setterName = "set" + capitalizeFirst( fieldName );

		StringBuilder sb = new StringBuilder();
		sb.append( "public void " ).append( setterName ).append( "(" )
				.append( fieldClass.getName() ).append( " " ).append( fieldName )
				.append( ")" ).append( "{" ).append( "this.theMap.put(\"" ).append( fieldName )
				.append( "\"," ).append( fieldName ).append( ")" ).append( ";" ).append( "}" );
		return CtMethod.make( sb.toString(), declaringClass );
	}

	private static CtClass resolveCtClass(Class clazz) throws NotFoundException {
		return resolveCtClass( clazz.getName() );
	}


	private static CtClass resolveCtClass(String clazz) throws NotFoundException {
		try {
			ClassPool pool = ClassPool.getDefault();
			return pool.get( clazz );
		}
		catch (NotFoundException e) {
			return null;
		}
	}

}
