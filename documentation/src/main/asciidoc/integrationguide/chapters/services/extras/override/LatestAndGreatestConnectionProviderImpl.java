import java.lang.Override;

public class LatestAndGreatestConnectionProviderImpl
    implements ConnectionProvider, Startable, Stoppable, Configurable {

    private LatestAndGreatestPoolBuilder lagPoolBuilder;

    private LatestAndGreatestPool lagPool;

    private boolean available = false;

    @Override
    public void configure(Map configurationValues) {
        // extract our config from the settings map
        lagPoolBuilder = buildBuilder( configurationValues );
    }

    @Override
    public void start() {
        // start the underlying pool
        lagPool = lagPoolBuilder.buildPool();

        available = true;
    }

    @Override
    public void stop() {
        available = false;

        // stop the underlying pool
        lagPool.shutdown();
    }

    @Override
    public Connection getConnection() throws SQLException {
        if ( !available ) {
            throwException(
                "LatestAndGreatest ConnectionProvider not available for use" )
        }

        return lagPool.borrowConnection();
    }

    @Override
    public void closeConnection(Connection conn) throws SQLException {
        if ( !available ) {
            warn(
                "LatestAndGreatest ConnectionProvider not available for use" )
        }

        if ( conn == null ) {
            return;
        }

        lagPool.releaseConnection( conn );
    }

    ...
}