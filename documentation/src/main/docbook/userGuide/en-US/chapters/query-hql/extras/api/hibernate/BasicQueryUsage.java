Query query = ...;
// in seconds
query.setTimeout( 2 );
// write to L2 caches, but do not read from them
query.setCacheMode( CacheMode.REFRESH );
// assuming query cache was enabled for the SessionFactory
query.setCacheable( true );
// add a comment to the generated SQL if enabled with the SF
query.setComment( "e pluribus unum" )