package com.netflix.exhibitor.core.utils;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test output of PatternLayoutEscapedTest
 * It should be escaping new lines, quotes, tabs and backslashes
 * This is necessary when we are logging these messages out as JSON objects
 */
public class TestPatternLayoutEscaped
{
    private Logger logger = Logger.getLogger(this.getClass());
    private PatternLayout layout = new PatternLayoutEscaped();

    @BeforeMethod
    public void setup()
    {
        layout.setConversionPattern("%m");
    }

    @Test
    public void testNewLine()
    {
        LoggingEvent event = createMessageEvent("This message contains \n new lines");
        Assert.assertTrue(layout.format(event).equals("This message contains \\n new lines"));
    }

    @Test
    public void testQuote()
    {
        LoggingEvent event = createMessageEvent("This message contains \" quotes");
        Assert.assertTrue(layout.format(event).equals("This message contains \\\" quotes"));
    }

    @Test
    public void testTab()
    {
        LoggingEvent event = createMessageEvent("This message contains a tab \t");
        Assert.assertTrue(layout.format(event).equals("This message contains a tab \\t"));
    }

    @Test
    public void testBackSlash()
    {
        LoggingEvent event = createMessageEvent("This message contains a backslash \\");
        Assert.assertTrue(layout.format(event).equals("This message contains a backslash \\\\"));
    }

    private LoggingEvent createMessageEvent(String message)
    {
        return new LoggingEvent(this.getClass().getCanonicalName(),
        logger,
        0,
        Level.toLevel("INFO"),
        message,
        new Exception());
    }
}