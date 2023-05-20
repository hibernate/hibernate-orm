CREATE OR REPLACE FUNCTION greater_than(count BIGINT, value NUMERIC, gr_val NUMERIC)
    RETURNS BIGINT AS
$$
BEGIN
    RETURN CASE WHEN value > gr_val THEN (count + 1)::BIGINT ELSE count::BIGINT END;
END;
$$ LANGUAGE "plpgsql";

CREATE OR REPLACE FUNCTION agg_final(c bigint) RETURNS BIGINT AS
$$
BEGIN
    return c;
END;
$$ LANGUAGE "plpgsql";

CREATE OR REPLACE AGGREGATE count_items_greater_val(NUMERIC, NUMERIC) (
    SFUNC = greater_than,
    STYPE = BIGINT,
    FINALFUNC = agg_final,
    INITCOND = 0);
