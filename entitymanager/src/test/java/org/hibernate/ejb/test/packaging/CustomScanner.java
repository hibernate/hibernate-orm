package org.hibernate.ejb.test.packaging;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Set;

import org.hibernate.ejb.packaging.NamedInputStream;
import org.hibernate.ejb.packaging.NativeScanner;
import org.hibernate.ejb.packaging.Scanner;

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
