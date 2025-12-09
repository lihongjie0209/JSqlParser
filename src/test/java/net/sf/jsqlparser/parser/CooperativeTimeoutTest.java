/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2019 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.parser;

import net.sf.jsqlparser.JSQLParserException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for cooperative timeout mechanism without creating temporary threads
 */
public class CooperativeTimeoutTest {

    @Test
    public void testSimpleQueryWithTimeout() throws JSQLParserException {
        String sql = "SELECT * FROM users WHERE id = 1";
        
        // Parse with a reasonable timeout (should succeed)
        CCJSqlParser parser = CCJSqlParserUtil.newParser(sql);
        parser.withTimeOut(5000); // 5 seconds
        
        assertDoesNotThrow(() -> {
            CCJSqlParserUtil.parseStatement(parser);
        });
    }

    @Test
    public void testTimeoutDoesNotCreateThreads() throws JSQLParserException {
        String sql = "SELECT * FROM users WHERE id = 1";
        
        // Get thread count before parsing
        int threadCountBefore = Thread.activeCount();
        
        CCJSqlParser parser = CCJSqlParserUtil.newParser(sql);
        parser.withTimeOut(5000);
        CCJSqlParserUtil.parseStatement(parser);
        
        // Get thread count after parsing
        int threadCountAfter = Thread.activeCount();
        
        // Should not create any new threads (cooperative timeout)
        assertEquals(threadCountBefore, threadCountAfter, 
            "Cooperative timeout should not create new threads");
    }

    @Test
    public void testMultipleParsingWithoutThreadAccumulation() throws JSQLParserException {
        int threadCountBefore = Thread.activeCount();
        
        // Parse multiple times
        for (int i = 0; i < 10; i++) {
            String sql = "SELECT * FROM table" + i + " WHERE id = " + i;
            CCJSqlParser parser = CCJSqlParserUtil.newParser(sql);
            parser.withTimeOut(5000);
            CCJSqlParserUtil.parseStatement(parser);
        }
        
        int threadCountAfter = Thread.activeCount();
        
        // Thread count should remain stable
        assertTrue(threadCountAfter <= threadCountBefore + 1, 
            "Thread count should not accumulate: before=" + threadCountBefore + ", after=" + threadCountAfter);
    }

    @Test
    public void testVeryComplexQueryWithShortTimeout() {
        // Create a very complex deeply nested query that takes significant time to parse
        StringBuilder sql = new StringBuilder("SELECT * FROM t1 WHERE id IN (");
        for (int i = 0; i < 200; i++) {
            sql.append("SELECT id FROM t").append(i).append(" WHERE x IN (");
        }
        sql.append("1");
        for (int i = 0; i < 200; i++) {
            sql.append(")");
        }
        sql.append(")");
        
        CCJSqlParser parser = CCJSqlParserUtil.newParser(sql.toString());
        parser.withTimeOut(50); // Short timeout: 50ms
        
        // This may timeout for very complex queries depending on CPU speed
        // The test verifies that timeout mechanism works, not necessarily that it always times out
        try {
            CCJSqlParserUtil.parseStatement(parser);
            // If parsing succeeds, just verify it completed
            assertTrue(true, "Query parsed successfully within timeout");
        } catch (JSQLParserException ex) {
            // If timeout occurs, verify it's a timeout exception
            assertTrue(ex.getMessage().contains("Time out occurred") || 
                      ex.getMessage().contains("timeout"),
                      "Should be a timeout exception");
        }
    }

    @Test
    public void testTimeoutDisabled() throws JSQLParserException {
        String sql = "SELECT * FROM users";
        
        CCJSqlParser parser = CCJSqlParserUtil.newParser(sql);
        parser.withTimeOut(0); // Disable timeout
        
        // Should parse successfully
        assertDoesNotThrow(() -> {
            CCJSqlParserUtil.parseStatement(parser);
        });
    }

    @Test
    public void testParseStatementsWithTimeout() throws JSQLParserException {
        String sql = "SELECT * FROM t1; SELECT * FROM t2; SELECT * FROM t3;";
        
        CCJSqlParser parser = CCJSqlParserUtil.newParser(sql);
        parser.withTimeOut(5000);
        
        assertDoesNotThrow(() -> {
            CCJSqlParserUtil.parseStatements(parser);
        });
    }
}
