/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * A utility class for logging event and status messages. It maintains a
 * collection of streams for different types of messages, but any messages with
 * unknown or unspecified stream go to the default stream.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: Logger.java,v 1.8 2006/11/09 06:01:43 rickknowles Exp $
 */
public class Logger {

    public final static String DEFAULT_STREAM = "Winstone";
    public final static Level MIN = Level.OFF;
    public final static Level ERROR = Level.SEVERE;
    public final static Level WARNING = Level.WARNING;
    public final static Level INFO = Level.INFO;
    public final static Level SPEED = Level.FINE;
    public final static Level DEBUG = Level.FINER;
    public final static Level FULL_DEBUG = Level.FINEST;
    public final static Level MAX = Level.ALL;

    protected final static Object semaphore = new Object();
    static boolean initialised = false;
    static Writer defaultStream;
    static Map<String,Writer> namedStreams;
//    protected static Collection nullStreams;
    static boolean showThrowingThread;

    /**
     * Initialises default streams
     */
    public static void init(Level level) {
        init(level, System.out, Charset.defaultCharset(), false);
    }

    public static void init(int level) {
        init(Level.parse(String.valueOf(level)));
    }

    /**
     * Initialises default streams
     *
     * @deprecated use {@link #init(Level, OutputStream, Charset, boolean)}
     */
    @Deprecated
    public static void init(
            Level level, OutputStream defaultStream, boolean showThrowingThreadArg) {
        init(level, defaultStream, Charset.defaultCharset(), showThrowingThreadArg);
    }

    /**
     * Initialize default streams
     */
    public static void init(
            Level level,
            OutputStream defaultStream,
            Charset defaultStreamCharset,
            boolean showThrowingThreadArg) {
        synchronized (semaphore) {
            if (!initialised) { // recheck in case we were blocking on another init
                initialised = false;
                LOGGER.setLevel(level);
                namedStreams = new HashMap<>();
//                nullStreams = new ArrayList();
                initialised = true;
                setStream(DEFAULT_STREAM, defaultStream, defaultStreamCharset);
                showThrowingThread = showThrowingThreadArg;
            }
        }
    }

    /**
     * Allocates a stream for redirection to a file etc
     *
     * @deprecated Use {@link #setStream(String, OutputStream, Charset)}
     */
    @Deprecated
    public static void setStream(String name, OutputStream stream) {
        setStream(name, stream, Charset.defaultCharset());
    }

    /**
     * Allocate a stream for redirection to a file etc
     */
    public static void setStream(String name, OutputStream stream, Charset charset) {
        setStream(name, stream != null ? new OutputStreamWriter(stream, charset) : null);
    }

    /**
     * Allocates a stream for redirection to a file etc
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_STATIC_REP2", justification = "False positive, this is intended")
    public static void setStream(String name, Writer stream) {
        if (name == null) {
            name = DEFAULT_STREAM;
        }
        if (!initialised) {
            init(INFO);
        }
        synchronized (semaphore) {
            if (name.equals(DEFAULT_STREAM)) {
                defaultStream = stream;
            } else if (stream == null) {
                namedStreams.remove(name);
            } else {
                namedStreams.put(name, stream);
            }
        }
    }

    /**
     * Forces a flush of the contents to file, display, etc
     */
    public static void flush(String name) {
        if (!initialised) {
            init(INFO);
        }

        Writer stream = getStreamByName(name);
        if (stream != null) {
            try {stream.flush();} catch (IOException err) {}
        }
    }

    private static Writer getStreamByName(String streamName) {
        if ((streamName != null) && streamName.equals(DEFAULT_STREAM)) {
            // As long as the stream has not been nulled, assign the default if not found
            synchronized (semaphore) {
                Writer stream = (Writer) namedStreams.get(streamName);
                if ((stream == null) && !namedStreams.containsKey(streamName)) {
                    stream = defaultStream;
                }
                return stream;
            }
        } else {
            return defaultStream;
        }

    }

    public static void setCurrentDebugLevel(int level) {
        if (!initialised) {
            init(level);
        } else synchronized (semaphore) {
            LOGGER.setLevel(Level.parse(String.valueOf(level)));
        }
    }

    /**
     * Writes a log message to the requested stream, and immediately flushes
     * the contents of the stream.
     */
    private static void logInternal(Level level, String message, Throwable error) {
        if (!initialised) {
            init(INFO);
        }

        String msg = "";
        if (showThrowingThread) {
            msg = "["+Thread.currentThread().getName()+"] - ";
        }
        msg += message;

        LOGGER.log(level,msg,error);
    }

    public static void log(Level level, WinstoneResourceBundle resources,
            String messageKey) {
        if (!LOGGER.isLoggable(level)) {
            return;
        } else {
            logInternal(level, resources.getString(messageKey), null);
        }
    }

    public static void log(Level level, WinstoneResourceBundle resources,
            String messageKey, Throwable error) {
        if (!LOGGER.isLoggable(level)) {
            return;
        } else {
            logInternal(level, resources.getString(messageKey), error);
        }
    }

    public static void log(Level level, WinstoneResourceBundle resources,
            String messageKey, Object param) {
        if (!LOGGER.isLoggable(level)) {
            return;
        } else {
            logInternal(level, resources.getString(messageKey, param), null);
        }
    }

    public static void log(Level level, WinstoneResourceBundle resources,
            String messageKey, Object... params) {
        if (!LOGGER.isLoggable(level)) {
            return;
        } else {
            logInternal(level, resources.getString(messageKey, params), null);
        }
    }

    public static void log(Level level, WinstoneResourceBundle resources,
            String messageKey, Object param, Throwable error) {
        if (!LOGGER.isLoggable(level)) {
            return;
        } else {
            logInternal(level, resources.getString(messageKey, param), error);
        }
    }

    public static void log(Level level, WinstoneResourceBundle resources,
            String messageKey, Object[] params, Throwable error) {
        if (!LOGGER.isLoggable(level)) {
            return;
        } else {
            logInternal(level, resources.getString(messageKey, params), error);
        }
    }

    public static void log(Level level, WinstoneResourceBundle resources,
            String streamName, String messageKey, Object[] params, Throwable error) {
        if (!LOGGER.isLoggable(level)) {
            return;
        } else {
            logInternal(level, resources.getString(messageKey, params), error);
        }
    }

    public static void logDirectMessage(Level level, String streamName, String message,
            Throwable error) {
        if (!LOGGER.isLoggable(level)) {
            return;
        } else {
            logInternal(level, message, error);
        }
    }

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("winstone");
}
