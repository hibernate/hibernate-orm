/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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

package org.hibernate.gradle.testing.database;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.DefaultRepositoryCacheManager;
import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.gradle.api.Project;
import org.gradle.api.internal.artifacts.IvyService;
import org.gradle.api.internal.artifacts.configurations.DefaultConfigurationContainer;
import org.gradle.api.internal.artifacts.configurations.ResolverProvider;
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency;
import org.gradle.api.internal.artifacts.ivyservice.DefaultIvyService;
import org.gradle.api.internal.artifacts.ivyservice.ErrorHandlingIvyService;
import org.gradle.api.internal.artifacts.ivyservice.IvyFactory;
import org.gradle.api.internal.artifacts.ivyservice.SettingsConverter;
import org.gradle.api.internal.artifacts.ivyservice.ShortcircuitEmptyConfigsIvyService;
import org.gradle.api.internal.artifacts.repositories.InternalRepository;
import org.gradle.api.internal.file.AbstractFileCollection;
import org.gradle.invocation.DefaultGradle;

import org.hibernate.gradle.util.BuildException;
import org.hibernate.gradle.util.ResolutionException;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class IvyXmlDependency extends DefaultSelfResolvingDependency {
	private final String name;

	public IvyXmlDependency(Project project, File ivyXmlFile) {
		super( new IvyXmlDependencyFileCollection( project, ivyXmlFile ) );
		this.name = ivyXmlFile.getParentFile().getName() + '/' + ivyXmlFile.getName();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public IvyXmlDependencyFileCollection getSource() {
		return (IvyXmlDependencyFileCollection) super.getSource();
	}

	private static class IvyXmlDependencyFileCollection extends AbstractFileCollection {
		private final Project project;
		private final File ivyXmlFile;
		private Set<File> resolvedDependencyFiles;

		private IvyXmlDependencyFileCollection(Project project, File ivyXmlFile) {
			this.project = project;
			this.ivyXmlFile = ivyXmlFile;
		}

		public File getIvyXmlFile() {
			return ivyXmlFile;
		}

		@Override
		public String getDisplayName() {
			return ivyXmlFile.getAbsolutePath();
		}

		public Set<File> getFiles() {
			if ( resolvedDependencyFiles == null ) {
				resolvedDependencyFiles = resolveDependencyFiles();
			}
			return resolvedDependencyFiles;
		}

		private Set<File> resolveDependencyFiles() {
			DefaultIvyService ivyService = unwrap(
					( (DefaultConfigurationContainer) project.getConfigurations() ).getIvyService()
			);

			final ModuleDescriptor moduleDescriptor = resolveIvyXml( ivyService );

			DefaultRepositoryCacheManager repositoryCacheManager = null;
			final ResolverProvider resolverProvider = ivyService.getResolverProvider();
			for ( DependencyResolver resolver : resolverProvider.getResolvers() ) {
				RepositoryCacheManager potentialRepositoryCacheManager = resolver.getRepositoryCacheManager();
				if ( DefaultRepositoryCacheManager.class.isInstance( potentialRepositoryCacheManager ) ) {
					repositoryCacheManager = (DefaultRepositoryCacheManager) potentialRepositoryCacheManager;
				}
			}
			if ( repositoryCacheManager == null ) {
				throw buildResolutionException( moduleDescriptor.getModuleRevisionId(), "Unable to locate proper dependency cache manager" );
			}

			HashSet<File> dependencyFiles = new HashSet<File>();
			for ( DependencyDescriptor dependencyDescriptor : moduleDescriptor.getDependencies() ) {
				final ModuleRevisionId info = dependencyDescriptor.getDynamicConstraintDependencyRevisionId();

				dependencyFiles.add( repositoryCacheManager.getIvyFileInCache( info ) );
			}

			return dependencyFiles;
		}

		private DefaultIvyService unwrap(IvyService ivyService) {
			if ( DefaultIvyService.class.isInstance( ivyService ) ) {
				return (DefaultIvyService) ivyService;
			}

			if ( ErrorHandlingIvyService.class.isInstance( ivyService ) ) {
				return unwrap( ( (ErrorHandlingIvyService) ivyService ).getIvyService() );
			}

			if ( ShortcircuitEmptyConfigsIvyService.class.isInstance( ivyService ) ) {
				return unwrap( ( (ShortcircuitEmptyConfigsIvyService) ivyService ).getIvyService() );
			}

			throw new BuildException( "Do not know how to extract needed ivy config from ivy service of type " + ivyService.getClass().getName() );
		}

		private ModuleDescriptor resolveIvyXml(DefaultIvyService ivyService) {
			Ivy ivy = locateIvyService( ivyService );
			ResolveReport resolveReport = performResolve( ivy, ivyXmlFile );
			return resolveReport.getModuleDescriptor();
		}

		@SuppressWarnings({ "unchecked" })
		private ResolveReport performResolve(Ivy ivy, File ivyXmlFile) {
			try {
				ResolveReport resolveReport = ivy.resolve( ivyXmlFile );
				if ( resolveReport.hasError() ) {
					throw buildResolutionException(
							resolveReport.getModuleDescriptor().getModuleRevisionId(),
							resolveReport.getAllProblemMessages()
					);
				}
				return resolveReport;
			}
			catch ( ParseException e ) {
				throw new BuildException( "Malformed ivy dependency file [" + ivyXmlFile.getName() + "]", e );
			}
			catch ( IOException e ) {
				throw new BuildException( "Problem reading ivy dependency file [" + ivyXmlFile.getName() + "]", e );
			}
		}

		private Ivy locateIvyService(DefaultIvyService ivyService) {
			final IvyFactory ivyFactory = ivyService.getIvyFactory();
			final SettingsConverter settingsConverter = ivyService.getSettingsConverter();
			final ResolverProvider resolverProvider = ivyService.getResolverProvider();

			// Ugh, can absolutely find no way to access this other than a bunch of recursive reflection calls to
			// locate the build's org.gradle.api.internal.project.TopLevelBuildServiceRegistry and access its
			// private org.gradle.api.internal.project.TopLevelBuildServiceRegistry.clientModuleRegistry field.
			//
			// Not sure if it is critical to reuse that map instance or not.  seems to work without, but i have no
			// idea about the ramifications.
			Map<String, ModuleDescriptor> clientModuleRegistry = new HashMap<String, ModuleDescriptor>();

			// this part is mainly DefaultIvyService#ivyForResolve
			IvySettings ivySettings = settingsConverter.convertForResolve(
					resolverProvider.getResolvers(),
					project.getGradle().getGradleUserHomeDir(),
					getGradeService( InternalRepository.class ),
					clientModuleRegistry
			);
			return ivyFactory.createIvy( ivySettings );
		}

		protected <T> T getGradeService(Class<T> type) {
			return ( (DefaultGradle) project.getGradle() ).getServices().get( type );
		}
	}

	private static ResolutionException buildResolutionException(ModuleRevisionId descriptor, List<String> messages) {
		return new ResolutionException( descriptor.toString(), messages );
	}

	private static ResolutionException buildResolutionException(ModuleRevisionId descriptor, String... messages) {
		return buildResolutionException( descriptor, Arrays.asList( messages ) );
	}
}
