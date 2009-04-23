/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.annotations.common.reflection.java;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.common.reflection.AnnotationReader;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMethod;
import org.hibernate.annotations.common.reflection.XPackage;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.reflection.MetadataProviderInjector;
import org.hibernate.annotations.common.reflection.MetadataProvider;
import org.hibernate.annotations.common.reflection.java.generics.IdentityTypeEnvironment;
import org.hibernate.annotations.common.reflection.java.generics.TypeEnvironment;
import org.hibernate.annotations.common.reflection.java.generics.TypeEnvironmentFactory;
import org.hibernate.annotations.common.reflection.java.generics.TypeSwitch;
import org.hibernate.annotations.common.reflection.java.generics.TypeUtils;
import org.hibernate.annotations.common.Version;
import org.hibernate.annotations.common.util.ReflectHelper;

/**
 * The factory for all the objects in this package.
 *
 * @author Paolo Perrotta
 * @author Davide Marchignoli
 * @author Emmanuel Bernard
 */
public class JavaReflectionManager implements ReflectionManager, MetadataProviderInjector {

	private MetadataProvider metadataProvider;

	public MetadataProvider getMetadataProvider() {
		if (metadataProvider == null) {
			setMetadataProvider( new JavaMetadataProvider() );
		}
		return metadataProvider;
	}

	public void setMetadataProvider(MetadataProvider metadataProvider) {
		this.metadataProvider = metadataProvider;
	}

	static {
		Version.touch();
	}

	private static class TypeKey extends Pair<Type, TypeEnvironment> {
		TypeKey(Type t, TypeEnvironment context) {
			super( t, context );
		}
	}

	private static class MemberKey extends Pair<Member, TypeEnvironment> {
		MemberKey(Member member, TypeEnvironment context) {
			super( member, context );
		}
	}

	private final Map<TypeKey, JavaXClass> xClasses = new HashMap<TypeKey, JavaXClass>();

	private final Map<Package, JavaXPackage> packagesToXPackages = new HashMap<Package, JavaXPackage>();

	private final Map<MemberKey, JavaXProperty> xProperties = new HashMap<MemberKey, JavaXProperty>();

	private final Map<MemberKey, JavaXMethod> xMethods = new HashMap<MemberKey, JavaXMethod>();

	private final TypeEnvironmentFactory typeEnvs = new TypeEnvironmentFactory();

	public XClass toXClass(Class clazz) {
		return toXClass( clazz, IdentityTypeEnvironment.INSTANCE );
	}

	public Class toClass(XClass xClazz) {
		if ( ! ( xClazz instanceof JavaXClass ) ) {
			throw new IllegalArgumentException( "XClass not coming from this ReflectionManager implementation" );
		}
		return (Class) ( (JavaXClass) xClazz ).toAnnotatedElement();
	}

	public Method toMethod(XMethod xMethod) {
		if ( ! ( xMethod instanceof JavaXMethod ) ) {
			throw new IllegalArgumentException( "XMethod not coming from this ReflectionManager implementation" );
		}
		return (Method) ( (JavaXAnnotatedElement) xMethod ).toAnnotatedElement();
	}

	public XClass classForName(String name, Class caller) throws ClassNotFoundException {
		return toXClass( ReflectHelper.classForName( name, caller ) );
	}

	public XPackage packageForName(String packageName) throws ClassNotFoundException {
		return getXAnnotatedElement( ReflectHelper.classForName( packageName + ".package-info" ).getPackage() );
	}

	XClass toXClass(Type t, final TypeEnvironment context) {
		return new TypeSwitch<XClass>() {
			@Override
			public XClass caseClass(Class classType) {
				TypeKey key = new TypeKey( classType, context );
				JavaXClass result = xClasses.get( key );
				if ( result == null ) {
					result = new JavaXClass( classType, context, JavaReflectionManager.this );
					xClasses.put( key, result );
				}
				return result;
			}

			@Override
			public XClass caseParameterizedType(ParameterizedType parameterizedType) {
				return toXClass( parameterizedType.getRawType(),
						typeEnvs.getEnvironment( parameterizedType, context )
				);
			}
		}.doSwitch( context.bind( t ) );
	}

	XPackage getXAnnotatedElement(Package pkg) {
		JavaXPackage xPackage = packagesToXPackages.get( pkg );
		if ( xPackage == null ) {
			xPackage = new JavaXPackage( pkg, this );
			packagesToXPackages.put( pkg, xPackage );
		}
		return xPackage;
	}

	XProperty getXProperty(Member member, TypeEnvironment context) {
		MemberKey key = new MemberKey( member, context );
        //FIXME get is as expensive as create most time spent in hashCode and equals
        JavaXProperty xProperty = xProperties.get( key );
		if ( xProperty == null ) {
			xProperty = JavaXProperty.create( member, context, this );
			xProperties.put( key, xProperty );
		}
		return xProperty;
	}

	XMethod getXMethod(Member member, TypeEnvironment context) {
		MemberKey key = new MemberKey( member, context );
        //FIXME get is as expensive as create most time spent in hashCode and equals
        JavaXMethod xMethod = xMethods.get( key );
		if ( xMethod == null ) {
			xMethod = JavaXMethod.create( member, context, this );
			xMethods.put( key, xMethod );
		}
		return xMethod;
	}

	TypeEnvironment getTypeEnvironment(final Type t) {
		return new TypeSwitch<TypeEnvironment>() {
			@Override
			public TypeEnvironment caseClass(Class classType) {
				return typeEnvs.getEnvironment( classType );
			}

			@Override
			public TypeEnvironment caseParameterizedType(ParameterizedType parameterizedType) {
				return typeEnvs.getEnvironment( parameterizedType );
			}

			@Override
			public TypeEnvironment defaultCase(Type type) {
				return IdentityTypeEnvironment.INSTANCE;
			}
		}.doSwitch( t );
	}

	public JavaXType toXType(TypeEnvironment context, Type propType) {
		Type boundType = toApproximatingEnvironment( context ).bind( propType );
		if ( TypeUtils.isArray( boundType ) ) {
			return new JavaXArrayType( propType, context, this );
		}
		if ( TypeUtils.isCollection( boundType ) ) {
			return new JavaXCollectionType( propType, context, this );
		}
		if ( TypeUtils.isSimple( boundType ) ) {
			return new JavaXSimpleType( propType, context, this );
		}
		throw new IllegalArgumentException( "No PropertyTypeExtractor available for type void " );
	}

	public boolean equals(XClass class1, Class class2) {
		if ( class1 == null ) {
			return class2 == null;
		}
		return ( (JavaXClass) class1 ).toClass().equals( class2 );
	}

	public TypeEnvironment toApproximatingEnvironment(TypeEnvironment context) {
		return typeEnvs.toApproximatingEnvironment( context );
	}

    public AnnotationReader buildAnnotationReader(AnnotatedElement annotatedElement) {
        return getMetadataProvider().getAnnotationReader( annotatedElement );
    }
    
    public Map getDefaults() {
        return getMetadataProvider().getDefaults();
    }

}
