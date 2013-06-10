package org.hibernate.test.instrument.cases;

/**
 * @author Steve Ebersole
 */
public interface Executable {
	void prepare();

	/**
	 * The reason that we need this method in the metamodel branch is because in this branch,
	 * we use {@link org.hibernate.boot.registry.classloading.spi.ClassLoaderService} to load entity classes
	 * (well, every classes), and by default, this service uses default hibernate classloader first, then
	 * it will bypass the instrument classloader, so here we have to explicitly provide this classloader to the
	 * {@link org.hibernate.boot.registry.classloading.spi.ClassLoaderService}.
	 *
 	 * @param instrumentedClassLoader
	 */
	void prepare(ClassLoader instrumentedClassLoader);
	void execute() throws Exception;
	void complete();
}
