import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.process.CommandLineArgumentProvider

/**
 * Supplies command line arguments computed lazily, so that build scripts do not need to look at
 * the project from a {@code doFirst} action (which the configuration cache does not allow).
 *
 * @author Steve Ebersole
 */
class LazyArgumentsProvider implements CommandLineArgumentProvider {
    @Internal
    Provider<? extends Iterable<String>> arguments

    @Override
    Iterable<String> asArguments() {
        arguments.get()
    }
}
