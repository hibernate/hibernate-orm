/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graalvm.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.ArrayList;

import org.hibernate.internal.util.ReflectHelper;

import com.oracle.svm.core.annotate.AutomaticFeature;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

/**
 * This is a best effort, untested experimental GraalVM feature to help people getting Hibernate ORM
 * to work with GraalVM native images.
 * There are multiple reasons for this to be untested. One is that for tests to be effective they would
 * need very extensive coverage of all functionality: the point of this class being a list of all things
 * being initialized reflectively, it's not possible to ensure that the list is comprehensive without the
 * tests being comprehensive as well.
 * The other problem is that this is listing just that "static needs" of Hibernate ORM: it will very likely
 * also need to access reflectively the user's domain model and the various extension points, depending on
 * configurations. Such configuration - and especially the domain model - is dynamic by its very own nature,
 * and therefore this list is merely provided as a useful starting point, but it needs to be extended;
 * such extensions could be automated, or will need to be explicitly passed to the native-image arguments.
 * <p>
 *     In conclusion, it's not possible to provide a fully comprehensive list: take this as a hopefully
 *     useful building block.
 * </p>
 * @author Sanne Grinovero
 */
@AutomaticFeature
public class GraalVMStaticAutofeature implements Feature {

	public void beforeAnalysis(Feature.BeforeAnalysisAccess before) {
		final Class<?>[] needsHavingSimpleConstructors = StaticClassLists.typesNeedingDefaultConstructorAccessible();
		final Class[] neddingAllConstructorsAccessible = StaticClassLists.typesNeedingAllConstructorsAccessible();
		//Size formula is just a reasonable guess:
		ArrayList<Executable> executables = new ArrayList<>( needsHavingSimpleConstructors.length + neddingAllConstructorsAccessible.length * 3 );
		for ( Class c : needsHavingSimpleConstructors ) {
			executables.add( ReflectHelper.getDefaultConstructor( c ) );
		}
		for ( Class c : neddingAllConstructorsAccessible ) {
			for ( Constructor declaredConstructor : c.getDeclaredConstructors() ) {
				executables.add( declaredConstructor );
			}
		}
		RuntimeReflection.register( needsHavingSimpleConstructors );
		RuntimeReflection.register( neddingAllConstructorsAccessible );
		RuntimeReflection.register( StaticClassLists.typesNeedingArrayCopy() );
		RuntimeReflection.register( executables.toArray(new Executable[0]) );
	}

}
