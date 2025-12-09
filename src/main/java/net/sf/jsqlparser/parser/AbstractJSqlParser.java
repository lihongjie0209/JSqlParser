/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2020 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.parser;

import java.util.ArrayList;
import java.util.List;

import net.sf.jsqlparser.parser.feature.Feature;
import net.sf.jsqlparser.parser.feature.FeatureConfiguration;

public abstract class AbstractJSqlParser<P> {

    protected int jdbcParameterIndex = 0;
    protected boolean errorRecovery = false;
    protected List<ParseException> parseErrors = new ArrayList<>();

    public P withSquareBracketQuotation(boolean allowSquareBracketQuotation) {
        return withFeature(Feature.allowSquareBracketQuotation, allowSquareBracketQuotation);
    }

    public P withAllowComplexParsing(boolean allowComplexParsing) {
      return withFeature(Feature.allowComplexParsing, allowComplexParsing);
    }

    public P withUnsupportedStatements(boolean allowUnsupportedStatements) {
        return withFeature(Feature.allowUnsupportedStatements, allowUnsupportedStatements);
    }

    public P withTimeOut(int timeOutMillSeconds) {
        return withFeature(Feature.timeOut, timeOutMillSeconds);
    }

    public P withBackslashEscapeCharacter(boolean allowBackslashEscapeCharacter) {
        return withFeature(Feature.allowBackslashEscapeCharacter, allowBackslashEscapeCharacter);
    }
    
    public P withFeature(Feature f, boolean enabled) {
        getConfiguration().setValue(f, enabled);
        return me();
    }

    public P withFeature(Feature f, int value) {
        getConfiguration().setValue(f, value);
        return me();
    }

    public abstract FeatureConfiguration getConfiguration();

    public abstract P me();

    public boolean getAsBoolean(Feature f) {
        return getConfiguration().getAsBoolean(f);
    }

    public Integer getAsInteger(Feature f) {
        return getConfiguration().getAsInteger(f);
    }

    public void setErrorRecovery(boolean errorRecovery) {
        this.errorRecovery = errorRecovery;
    }

    public List<ParseException> getParseErrors() {
        return parseErrors;
    }

    /**
     * Start the timeout timer for cooperative timeout mechanism.
     * This should be called before parsing starts.
     */
    public void startTimeout() {
        this.timeoutMillis = getConfiguration().getAsInteger(Feature.timeOut);
        this.parseStartTime = System.currentTimeMillis();
        this.interrupted = false;
    }

    /**
     * Check if parsing has exceeded the timeout.
     * This method should be called periodically during parsing.
     * 
     * @return true if timeout has been exceeded
     */
    public boolean checkTimeout() {
        if (timeoutMillis <= 0) {
            return false;
        }
        
        long elapsed = System.currentTimeMillis() - parseStartTime;
        if (elapsed > timeoutMillis) {
            interrupted = true;
            return true;
        }
        return false;
    }

    /**
     * Reset timeout state after parsing completes.
     */
    public void resetTimeout() {
        this.parseStartTime = 0;
        this.interrupted = false;
    }

}
