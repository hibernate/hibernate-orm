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

package org.hibernate.gradle.util;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.AbstractMessageLogger;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.MessageLogger;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.artifacts.IvyService;
import org.gradle.api.internal.artifacts.configurations.DefaultConfigurationContainer;
import org.gradle.api.internal.artifacts.configurations.ResolverProvider;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;
import org.gradle.api.internal.artifacts.ivyservice.DefaultIvyService;
import org.gradle.api.internal.artifacts.ivyservice.ErrorHandlingIvyService;
import org.gradle.api.internal.artifacts.ivyservice.IvyFactory;
import org.gradle.api.internal.artifacts.ivyservice.SettingsConverter;
import org.gradle.api.internal.artifacts.ivyservice.ShortcircuitEmptyConfigsIvyService;
import org.gradle.api.internal.artifacts.repositories.InternalRepository;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.invocation.DefaultGradle;

/**
 * A helper for handling resolution of ivy.xml files through Ivy calls using the bits and pieces of Ivy config
 * from Gradle.
 *
 * @author Steve Ebersole
 */
public class IvyResolutionHelper {
    private static final Logger log = Logging.getLogger( IvyResolutionHelper.class );

    private final Project project;

    /**
     * Creates an Ivy resolution helper for the given project.  The project will be the one on
     * which the requested configurations are created/used.
     *
     * @param project The project into which to inject the resolved dependencies.
     */
    public IvyResolutionHelper(Project project) {
        this.project = project;
    }

    /**
     * Constitutes the main API.  Resolve the dependencies named in the given ivy.xml file into the named
     * configuration.
     *
     * @param ivyXmlFile The ivy.xml file
     * @param configurationName The name of the configuration into which to place the resolved dependencies.
     *
     * @return The configuration. Returns null to indicate a problem doing the resolution.
     */
    public Configuration resolve(File ivyXmlFile, String configurationName) {
        log.debug( "Resolving file[{}] by configuration[{}]", ivyXmlFile, configurationName );
        ResolveReport resolveReport = performResolve( getIvy(), ivyXmlFile );
        ModuleDescriptor moduleDescriptor = resolveReport.getModuleDescriptor();
        Configuration configuration = getOrCreateConfiguration( configurationName );
        for ( DependencyDescriptor dependencyDescriptor : moduleDescriptor.getDependencies() ) {
            final ModuleRevisionId info = dependencyDescriptor.getDynamicConstraintDependencyRevisionId();
            final Dependency dependency = new DefaultExternalModuleDependency(
                    info.getOrganisation(),
                    info.getName(),
                    info.getRevision()
            );
            configuration.addDependency( dependency );
            log.debug( "Added dependency {} to configuration {}", dependency, configurationName );
        }

        return configuration;
    }

    public Configuration getOrCreateConfiguration(String configurationName) {
        Configuration configuration = project.getConfigurations().findByName( configurationName );
        if ( configuration == null ) {
            configuration = project.getConfigurations().add( configurationName );
        }
        return configuration;
    }

    @SuppressWarnings( { "unchecked" })
    protected ResolveReport performResolve(Ivy ivy, File ivyXmlFile) {
        final MessageLogger originalMessageLogger = Message.getDefaultLogger();
        Message.setDefaultLogger( new NoOpMessageLogger() );
        try {
            ResolveReport resolveReport = ivy.resolve( ivyXmlFile );
            if ( resolveReport.hasError() ) {
                throw new ResolutionException(
                        resolveReport.getModuleDescriptor().getModuleRevisionId().toString(),
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
        finally {
            Message.setDefaultLogger( originalMessageLogger );
        }
    }

    /**
     * Create an {@link Ivy} instance based on the running Gradle instance.
     * <p/>
     * Much of this is copied from {@link org.gradle.api.internal.artifacts.ivyservice.DefaultIvyService} because it
     * unfortunately does not expose the needed information.
     *
     * @return The generated {@link Ivy} instance
     */
    private Ivy getIvy() {
        DefaultIvyService ivyService = unwrap(
                ( (DefaultConfigurationContainer) project.getConfigurations() ).getIvyService()
        );

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

        throw new BuildException(
                "Do not know how to extract needed ivy config from ivy service of type " + ivyService.getClass()
                        .getName()
        );
    }

    protected <T> T getGradeService(Class<T> type) {
        return ( (DefaultGradle) project.getGradle() ).getServices().get( type );
    }

    private static class NoOpMessageLogger extends AbstractMessageLogger implements MessageLogger {
        public void log(String s, int i) {
        }

        public void rawlog(String s, int i) {
        }

        public void sumupProblems() {
        }

        protected void doProgress() {
        }

        protected void doEndProgress(String s) {
        }
    }
}
