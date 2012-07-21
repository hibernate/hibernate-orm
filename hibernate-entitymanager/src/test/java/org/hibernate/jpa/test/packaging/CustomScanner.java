package org.hibernate.jpa.test.packaging;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Set;

import org.hibernate.jpa.packaging.internal.NativeScanner;
import org.hibernate.jpa.packaging.spi.NamedInputStream;
import org.hibernate.jpa.packaging.spi.Scanner;

/**
 * @author Emmanuel Bernard
 */
public class CustomScanner implements Scanner {
	public static boolean isUsed = false;
	private Scanner scanner = new NativeScanner();

	public static boolean isUsed() {
		return isUsed;
	}

	public static void resetUsed() {
		isUsed = false;
	}

	public Set<Package> getPackagesInJar(URL jartoScan, Set<Class<? extends Annotation>> annotationsToLookFor) {
		isUsed = true;
		return scanner.getPackagesInJar( jartoScan, annotationsToLookFor );
	}

	public Set<Class<?>> getClassesInJar(URL jartoScan, Set<Class<? extends Annotation>> annotationsToLookFor) {
		isUsed = true;
		return scanner.getClassesInJar( jartoScan, annotationsToLookFor );
	}

	public Set<NamedInputStream> getFilesInJar(URL jartoScan, Set<String> filePatterns) {
		isUsed = true;
		return scanner.getFilesInJar( jartoScan, filePatterns );
	}

	public Set<NamedInputStream> getFilesInClasspath(Set<String> filePatterns) {
		isUsed = true;
		return scanner.getFilesInClasspath( filePatterns );
	}

	public String getUnqualifiedJarName(URL jarUrl) {
		isUsed = true;
		return scanner.getUnqualifiedJarName( jarUrl );
	}
}
