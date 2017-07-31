package com.netflix.exhibitor.core.utils;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * When loggin multiline messages to the journal(journald in this case)
 * only the first line is logged. Escaping \n solves the problem.
 */
public class PatternLayoutEscaped extends PatternLayout {

  public PatternLayoutEscaped(final String s) {
    super(s);
  }

  public PatternLayoutEscaped() {
    super();
  }

  @Override
  public String format(final LoggingEvent event) {
    if (event.getMessage() instanceof String) {
      return super.format(appendStackTraceToEvent(event));
    }
    return super.format(event);
  }

  /**
   * Create a copy of event, but append a stack trace to the message (if it exists). Then it escapes
   * the backslashes, tabs, newlines and quotes in its message as we are sending it as JSON and we
   * don't want any corruption of the JSON object.
   */
  private LoggingEvent appendStackTraceToEvent(final LoggingEvent event) {
    String message = event.getMessage().toString();
    // If there is a stack trace available, print it out
    if (event.getThrowableInformation() != null) {
      final String[] s = event.getThrowableStrRep();
      for (final String line : s) {
        message += "\n" + line;
      }
    }
    message = message
        .replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace("\"", "\\\"")
        .replace("\t", "\\t");

    final Throwable throwable = event.getThrowableInformation() == null ? null
        : event.getThrowableInformation().getThrowable();
    return new LoggingEvent(event.getFQNOfLoggerClass(),
        event.getLogger(),
        event.getTimeStamp(),
        event.getLevel(),
        message,
        throwable);
  }
}
